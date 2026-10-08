package com.interviewprep.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.persistence.EntityManagerFactory;
import java.math.BigDecimal;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser
class ProductControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ProductRepository repository;

    @Autowired
    ProductSeeder seeder;

    @Autowired
    CacheManager cacheManager;

    @Autowired
    EntityManagerFactory entityManagerFactory;

    Product smartLamp;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        cacheManager.getCache(ProductCacheConfig.PRODUCTS).clear();
        smartLamp = repository.save(new Product("Smart Lamp", "Home", new BigDecimal("25.00"), 10, 4.5));
        repository.save(new Product("Eco Lamp", "Home", new BigDecimal("15.00"), 0, 3.9));
        repository.save(new Product("Pro Speaker", "Electronics", new BigDecimal("120.00"), 5, 4.8));
        repository.save(new Product("Classic Notebook", "Books", new BigDecimal("8.50"), 50, 4.1));
        repository.save(new Product("Deluxe Lamp 100%", "Home", new BigDecimal("60.00"), 3, 4.0));
    }

    @Test
    void allFiltersCombineInOneRequest() throws Exception {
        mvc.perform(get("/api/products")
                        .param("category", "Home")
                        .param("minPrice", "10")
                        .param("maxPrice", "50")
                        .param("inStock", "true")
                        .param("q", "lamp"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Smart Lamp"));
    }

    @Test
    void eachFilterWorksOnItsOwn() throws Exception {
        mvc.perform(get("/api/products").param("category", "Home"))
                .andExpect(jsonPath("$.totalElements").value(3));
        mvc.perform(get("/api/products").param("q", "LAMP"))
                .andExpect(jsonPath("$.content[*].name",
                        containsInAnyOrder("Smart Lamp", "Eco Lamp", "Deluxe Lamp 100%")));
        mvc.perform(get("/api/products").param("inStock", "true"))
                .andExpect(jsonPath("$.totalElements").value(4));
        mvc.perform(get("/api/products").param("minPrice", "20").param("maxPrice", "100"))
                .andExpect(jsonPath("$.content[*].name", containsInAnyOrder("Smart Lamp", "Deluxe Lamp 100%")));
    }

    @Test
    void searchTreatsWildcardCharactersLiterally() throws Exception {
        mvc.perform(get("/api/products").param("q", "%"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Deluxe Lamp 100%"));
    }

    @Test
    void pagesAreSortedAndIncludeTotals() throws Exception {
        mvc.perform(get("/api/products").param("size", "2").param("page", "0").param("sort", "price,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].name").value("Pro Speaker"))
                .andExpect(jsonPath("$.content[1].name").value("Deluxe Lamp 100%"));

        mvc.perform(get("/api/products").param("sort", "name"))
                .andExpect(jsonPath("$.content[0].name").value("Classic Notebook"));
    }

    @Test
    void pageSizeIsCappedAt100() throws Exception {
        mvc.perform(get("/api/products").param("size", "500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(100));
    }

    @Test
    void invalidListRequestsReturn400() throws Exception {
        mvc.perform(get("/api/products").param("sort", "password"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("sort"));
        mvc.perform(get("/api/products").param("minPrice", "50").param("maxPrice", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("minPrice"));
        mvc.perform(get("/api/products").param("minPrice", "cheap"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("minPrice"));
    }

    @Test
    void lookupByIdAnd404() throws Exception {
        mvc.perform(get("/api/products/{id}", smartLamp.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Smart Lamp"))
                .andExpect(jsonPath("$.price").value(25.00));
        mvc.perform(get("/api/products/999999")).andExpect(status().isNotFound());
    }

    @Test
    void repeatedLookupsQueryTheDatabaseOnlyOnce() throws Exception {
        Statistics stats = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        stats.clear();

        for (int i = 0; i < 5; i++) {
            mvc.perform(get("/api/products/{id}", smartLamp.getId())).andExpect(status().isOk());
        }

        // Five lookups, one SQL statement: the other four were served from the cache.
        assertThat(stats.getPrepareStatementCount()).isEqualTo(1);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateIsVisibleOnTheNextLookup() throws Exception {
        mvc.perform(get("/api/products/{id}", smartLamp.getId()))
                .andExpect(jsonPath("$.price").value(25.00)); // now cached

        mvc.perform(put("/api/products/{id}", smartLamp.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Smart Lamp\",\"category\":\"Home\",\"price\":19.99,\"stock\":7,\"rating\":4.6}"))
                .andExpect(status().isOk());

        mvc.perform(get("/api/products/{id}", smartLamp.getId()))
                .andExpect(jsonPath("$.price").value(19.99))
                .andExpect(jsonPath("$.stock").value(7));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deletedProductIsNotServedFromTheCache() throws Exception {
        mvc.perform(get("/api/products/{id}", smartLamp.getId())).andExpect(status().isOk()); // now cached

        mvc.perform(delete("/api/products/{id}", smartLamp.getId())).andExpect(status().isNoContent());

        mvc.perform(get("/api/products/{id}", smartLamp.getId())).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void invalidUpdateReturnsFieldErrors() throws Exception {
        mvc.perform(put("/api/products/{id}", smartLamp.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"category\":\"Home\",\"price\":-1,\"stock\":-5,\"rating\":9}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[*].field",
                        containsInAnyOrder("name", "price", "stock", "rating")));
    }

    @Test
    void usersCannotChangeProducts() throws Exception {
        mvc.perform(put("/api/products/{id}", smartLamp.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"X\",\"category\":\"Home\",\"price\":1,\"stock\":1,\"rating\":1}"))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/products/{id}", smartLamp.getId())).andExpect(status().isForbidden());
    }

    @Test
    void seederCreates100Products() throws Exception {
        repository.deleteAll();

        seeder.run(null);

        assertThat(repository.count()).isEqualTo(ProductSeeder.PRODUCT_COUNT);
        mvc.perform(get("/api/products").param("size", "10"))
                .andExpect(jsonPath("$.totalElements").value(100))
                .andExpect(jsonPath("$.totalPages").value(10));
    }
}
