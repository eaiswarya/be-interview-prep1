package com.interviewprep.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewprep.model.Product;
import com.interviewprep.repository.OrderRepository;
import com.interviewprep.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@SpringBootTest
@AutoConfigureMockMvc
class OrderControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    ProductRepository productRepository;

    @BeforeEach
    void clean() {
        orderRepository.deleteAll();
    }

    @Test
    void fiftySimultaneousOrdersForStockTenSellExactlyTen() throws Exception {
        Product product = productWithStock(10);
        int customers = 50;

        List<Integer> statuses = runConcurrently(customers, i ->
                placeOrder(i + 1L, UUID.randomUUID().toString(), item(product.getId(), 1)).getStatus());

        assertThat(statuses).filteredOn(s -> s == 201).hasSize(10);
        assertThat(statuses).filteredOn(s -> s == 409).hasSize(40);
        assertThat(stockOf(product)).isZero();
        assertThat(orderRepository.count()).isEqualTo(10);
    }

    @Test
    void retryingTheSameRequestCreatesOneOrder() throws Exception {
        Product product = productWithStock(10);
        String key = UUID.randomUUID().toString();

        MockHttpServletResponse first = placeOrder(1L, key, item(product.getId(), 3));
        MockHttpServletResponse retry = placeOrder(1L, key, item(product.getId(), 3));

        assertThat(first.getStatus()).isEqualTo(201);
        assertThat(retry.getStatus()).isEqualTo(200);
        assertThat(idOf(retry)).isEqualTo(idOf(first));
        assertThat(stockOf(product)).isEqualTo(7);
        assertThat(orderRepository.count()).isEqualTo(1);
    }

    @Test
    void simultaneousRetriesStillCreateOneOrder() throws Exception {
        Product product = productWithStock(10);
        String key = UUID.randomUUID().toString();

        List<MockHttpServletResponse> responses = runConcurrently(10, i ->
                placeOrder(1L, key, item(product.getId(), 2)));

        assertThat(responses).filteredOn(r -> r.getStatus() == 201).hasSize(1);
        assertThat(responses).filteredOn(r -> r.getStatus() == 200).hasSize(9);
        assertThat(responses.stream().map(this::idOf).distinct()).hasSize(1);
        assertThat(stockOf(product)).isEqualTo(8);
        assertThat(orderRepository.count()).isEqualTo(1);
    }

    @Test
    void reusingAKeyForADifferentOrderIsRejected() throws Exception {
        Product product = productWithStock(10);
        String key = UUID.randomUUID().toString();
        placeOrder(1L, key, item(product.getId(), 1));

        MockHttpServletResponse different = placeOrder(1L, key, item(product.getId(), 5));

        assertThat(different.getStatus()).isEqualTo(409);
        assertThat(stockOf(product)).isEqualTo(9);
    }

    @Test
    void theSameKeyFromAnotherUserIsADifferentOrder() throws Exception {
        Product product = productWithStock(10);
        String key = UUID.randomUUID().toString();

        assertThat(placeOrder(1L, key, item(product.getId(), 1)).getStatus()).isEqualTo(201);
        assertThat(placeOrder(2L, key, item(product.getId(), 1)).getStatus()).isEqualTo(201);
        assertThat(stockOf(product)).isEqualTo(8);
    }

    @Test
    void anOrderReservesAllItemsOrNone() throws Exception {
        Product plenty = productWithStock(5);
        Product scarce = productWithStock(1);

        mvc.perform(order(1L, UUID.randomUUID().toString(), item(plenty.getId(), 2), item(scarce.getId(), 2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value(
                        "Insufficient stock for product " + scarce.getId() + ": requested 2, available 1"));

        assertThat(stockOf(plenty)).isEqualTo(5);
        assertThat(stockOf(scarce)).isEqualTo(1);
        assertThat(orderRepository.count()).isZero();
    }

    @Test
    void repeatedLinesForOneProductAreMerged() throws Exception {
        Product product = productWithStock(10);

        mvc.perform(order(1L, UUID.randomUUID().toString(), item(product.getId(), 2), item(product.getId(), 3)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].quantity").value(5));
        assertThat(stockOf(product)).isEqualTo(5);
    }

    @Test
    void cancellingReturnsTheStockOnce() throws Exception {
        Product product = productWithStock(10);
        long orderId = idOf(placeOrder(1L, UUID.randomUUID().toString(), item(product.getId(), 3)));
        assertThat(stockOf(product)).isEqualTo(7);

        mvc.perform(post("/api/orders/{id}/cancel", orderId).with(user(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        assertThat(stockOf(product)).isEqualTo(10);

        mvc.perform(post("/api/orders/{id}/cancel", orderId).with(user(1L)))
                .andExpect(status().isConflict());
        assertThat(stockOf(product)).isEqualTo(10);
    }

    @Test
    void productLookupShowsTheNewStockAfterAnOrder() throws Exception {
        Product product = productWithStock(10);
        mvc.perform(get("/api/products/{id}", product.getId()).with(user(1L)))
                .andExpect(jsonPath("$.stock").value(10)); // now cached

        placeOrder(1L, UUID.randomUUID().toString(), item(product.getId(), 4));

        mvc.perform(get("/api/products/{id}", product.getId()).with(user(1L)))
                .andExpect(jsonPath("$.stock").value(6));
    }

    @Test
    void usersCannotSeeOrCancelOtherUsersOrders() throws Exception {
        Product product = productWithStock(10);
        long orderId = idOf(placeOrder(1L, UUID.randomUUID().toString(), item(product.getId(), 1)));

        mvc.perform(get("/api/orders/{id}", orderId).with(user(1L))).andExpect(status().isOk());
        mvc.perform(get("/api/orders/{id}", orderId).with(user(2L))).andExpect(status().isNotFound());
        mvc.perform(post("/api/orders/{id}/cancel", orderId).with(user(2L))).andExpect(status().isNotFound());
        assertThat(stockOf(product)).isEqualTo(9);
    }

    @Test
    void invalidOrdersAreRejected() throws Exception {
        Product product = productWithStock(10);

        mvc.perform(post("/api/orders").with(user(1L)).contentType(MediaType.APPLICATION_JSON)
                        .content(body(item(product.getId(), 1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("Idempotency-Key"));
        mvc.perform(order(1L, UUID.randomUUID().toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("items"));
        mvc.perform(order(1L, UUID.randomUUID().toString(), item(product.getId(), 0)))
                .andExpect(status().isBadRequest());
        mvc.perform(order(1L, UUID.randomUUID().toString(), item(999_999L, 1)))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/orders").header("Idempotency-Key", "k").contentType(MediaType.APPLICATION_JSON)
                        .content(body(item(product.getId(), 1))))
                .andExpect(status().isUnauthorized());

        assertThat(orderRepository.count()).isZero();
        assertThat(stockOf(product)).isEqualTo(10);
    }

    private interface ConcurrentCall<T> {
        T run(int index) throws Exception;
    }

    /** Starts all tasks at the same instant and waits for every result. */
    private <T> List<T> runConcurrently(int threads, ConcurrentCall<T> task) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<T>> futures = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                int index = i;
                Callable<T> call = () -> {
                    start.await();
                    return task.run(index);
                };
                futures.add(pool.submit(call));
            }
            start.countDown();
            List<T> results = new ArrayList<>();
            for (Future<T> future : futures) {
                results.add(future.get());
            }
            return results;
        } finally {
            pool.shutdown();
        }
    }

    private Product productWithStock(int stock) {
        return productRepository.save(new Product("Test product", "Test", new BigDecimal("9.99"), stock, 4.0));
    }

    private int stockOf(Product product) {
        return productRepository.findById(product.getId()).orElseThrow().getStock();
    }

    private MockHttpServletResponse placeOrder(Long userId, String key, String... items) throws Exception {
        return mvc.perform(order(userId, key, items)).andReturn().getResponse();
    }

    private MockHttpServletRequestBuilder order(Long userId, String key, String... items) {
        return post("/api/orders").with(user(userId)).header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON).content(body(items));
    }

    private static RequestPostProcessor user(Long userId) {
        return jwt().jwt(token -> token.subject(userId.toString()));
    }

    private static String item(Long productId, int quantity) {
        return "{\"productId\":" + productId + ",\"quantity\":" + quantity + "}";
    }

    private static String body(String... items) {
        return "{\"items\":[" + String.join(",", items) + "]}";
    }

    private long idOf(MockHttpServletResponse response) {
        try {
            JsonNode node = json.readTree(response.getContentAsString());
            return node.get("id").asLong();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
