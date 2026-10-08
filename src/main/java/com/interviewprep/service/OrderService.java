package com.interviewprep.service;

import com.interviewprep.config.CacheConfig;
import com.interviewprep.dto.OrderItemRequest;
import com.interviewprep.dto.OrderRequest;
import com.interviewprep.dto.OrderResponse;
import com.interviewprep.dto.PlacedOrder;
import com.interviewprep.exception.ConflictException;
import com.interviewprep.exception.InsufficientStockException;
import com.interviewprep.exception.InvalidRequestException;
import com.interviewprep.exception.ResourceNotFoundException;
import com.interviewprep.model.Order;
import com.interviewprep.model.OrderItem;
import com.interviewprep.repository.OrderRepository;
import com.interviewprep.repository.ProductRepository;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class OrderService {

    static final int MAX_IDEMPOTENCY_KEY_LENGTH = 100;

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final TransactionTemplate transaction;
    private final Cache productCache;

    public OrderService(OrderRepository orderRepository, ProductRepository productRepository,
            PlatformTransactionManager transactionManager, CacheManager cacheManager) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.transaction = new TransactionTemplate(transactionManager);
        this.productCache = cacheManager.getCache(CacheConfig.PRODUCTS);
    }

    /**
     * Places an order exactly once per (user, Idempotency-Key).
     * A retry finds the existing order and returns it. Two retries racing each other both get past that
     * lookup, but only one can insert the key (unique constraint): the other's transaction rolls back,
     * including any stock it reserved, and it returns the winner's order.
     */
    public PlacedOrder place(Long userId, String idempotencyKey, OrderRequest request) {
        validateKey(idempotencyKey);
        SortedMap<Long, Integer> quantities = quantitiesByProduct(request);
        String fingerprint = fingerprint(quantities);

        var existing = orderRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey);
        if (existing.isPresent()) {
            return replay(existing.get(), fingerprint);
        }
        try {
            OrderResponse created = transaction.execute(status -> create(userId, idempotencyKey, fingerprint, quantities));
            return new PlacedOrder(created, true);
        } catch (DataIntegrityViolationException concurrentRetryWon) {
            Order winner = orderRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey)
                    .orElseThrow(() -> concurrentRetryWon);
            return replay(winner, fingerprint);
        }
    }

    @Transactional(readOnly = true)
    public OrderResponse get(Long userId, Long orderId) {
        return OrderResponse.from(find(userId, orderId));
    }

    /** Cancels a PLACED order and returns its stock, exactly once even if cancel is called concurrently. */
    @Transactional
    public OrderResponse cancel(Long userId, Long orderId) {
        Order order = find(userId, orderId);
        if (orderRepository.cancelIfPlaced(order.getId()) == 0) {
            throw new ConflictException("Order " + orderId + " is already cancelled");
        }
        for (OrderItem item : order.getItems()) {
            productRepository.releaseStock(item.getProductId(), item.getQuantity());
            productCache.evict(item.getProductId());
        }
        return OrderResponse.from(find(userId, orderId));
    }

    /**
     * Runs inside one transaction: the key is claimed first, then stock is reserved item by item. Any failure
     * rolls back everything, so an order reserves all of its items or none of them.
     */
    private OrderResponse create(Long userId, String key, String fingerprint, SortedMap<Long, Integer> quantities) {
        // Claiming the key first means a concurrent duplicate fails fast, before touching any stock.
        Order order = orderRepository.saveAndFlush(new Order(userId, key, fingerprint));

        // Sorted by product id, so two orders for the same products lock their rows in the same order
        // and cannot deadlock each other.
        for (Map.Entry<Long, Integer> entry : quantities.entrySet()) {
            Long productId = entry.getKey();
            int quantity = entry.getValue();
            if (productRepository.reserveStock(productId, quantity) == 0) {
                int available = productRepository.findById(productId)
                        .orElseThrow(() -> new ResourceNotFoundException("Product", productId))
                        .getStock();
                throw new InsufficientStockException(productId, quantity, available);
            }
            order.addItem(productId, quantity);
            // The product cache is transaction-aware: this eviction happens only after the commit.
            productCache.evict(productId);
        }
        orderRepository.flush();
        return OrderResponse.from(order);
    }

    private PlacedOrder replay(Order order, String fingerprint) {
        if (!order.getRequestFingerprint().equals(fingerprint)) {
            throw new ConflictException("Idempotency-Key " + order.getIdempotencyKey()
                    + " was already used for a different order");
        }
        return new PlacedOrder(OrderResponse.from(order), false);
    }

    private Order find(Long userId, Long orderId) {
        return orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
    }

    private static void validateKey(String key) {
        if (key.isBlank() || key.length() > MAX_IDEMPOTENCY_KEY_LENGTH) {
            throw new InvalidRequestException("Idempotency-Key",
                    "must be 1 to " + MAX_IDEMPOTENCY_KEY_LENGTH + " characters");
        }
    }

    /** Merges repeated lines for the same product and sorts by product id. */
    private static SortedMap<Long, Integer> quantitiesByProduct(OrderRequest request) {
        SortedMap<Long, Integer> quantities = new TreeMap<>();
        for (OrderItemRequest item : request.items()) {
            quantities.merge(item.productId(), item.quantity(), Integer::sum);
        }
        return quantities;
    }

    private static String fingerprint(SortedMap<Long, Integer> quantities) {
        return quantities.entrySet().stream()
                .map(entry -> entry.getKey() + "x" + entry.getValue())
                .collect(Collectors.joining(","));
    }
}
