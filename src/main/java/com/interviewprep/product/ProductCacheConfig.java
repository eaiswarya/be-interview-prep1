package com.interviewprep.product;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.transaction.TransactionAwareCacheManagerProxy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class ProductCacheConfig {

    public static final String PRODUCTS = "products";

    /**
     * In-process Caffeine cache, bounded so it cannot grow without limit.
     * Wrapped as transaction-aware: an eviction requested inside a transaction runs only after the commit,
     * so a reader can never re-cache the old row between the eviction and the commit.
     */
    @Bean
    CacheManager cacheManager() {
        CaffeineCacheManager caffeine = new CaffeineCacheManager(PRODUCTS);
        caffeine.setCaffeine(Caffeine.newBuilder().maximumSize(10_000));
        caffeine.setAllowNullValues(false);
        return new TransactionAwareCacheManagerProxy(caffeine);
    }
}
