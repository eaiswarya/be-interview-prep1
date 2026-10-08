package com.interviewprep.product;

import java.math.BigDecimal;
import java.time.Instant;

/** Immutable, so it is safe to share from the cache between requests. */
public record ProductResponse(
        Long id,
        String name,
        String category,
        BigDecimal price,
        int stock,
        double rating,
        Instant createdAt) {

    static ProductResponse from(Product product) {
        return new ProductResponse(product.getId(), product.getName(), product.getCategory(), product.getPrice(),
                product.getStock(), product.getRating(), product.getCreatedAt());
    }
}
