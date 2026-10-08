package com.interviewprep.product;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

/**
 * Builds one WHERE clause from whichever filters are present: each filter adds one condition and they are
 * ANDed together, so any combination works in a single query. Values are bound as parameters, never
 * concatenated into SQL.
 */
final class ProductSpecifications {

    private ProductSpecifications() {
    }

    static Specification<Product> matching(ProductFilter filter) {
        List<Specification<Product>> conditions = new ArrayList<>();
        if (hasText(filter.category())) {
            // Exact match keeps the category index usable.
            conditions.add(categoryIs(filter.category().trim()));
        }
        if (filter.minPrice() != null) {
            conditions.add(priceAtLeast(filter.minPrice()));
        }
        if (filter.maxPrice() != null) {
            conditions.add(priceAtMost(filter.maxPrice()));
        }
        if (Boolean.TRUE.equals(filter.inStock())) {
            conditions.add(inStock());
        }
        if (hasText(filter.q())) {
            conditions.add(nameContains(filter.q().trim()));
        }
        return Specification.allOf(conditions);
    }

    static Specification<Product> categoryIs(String category) {
        return (root, query, cb) -> cb.equal(root.get("category"), category);
    }

    static Specification<Product> priceAtLeast(BigDecimal min) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("price"), min);
    }

    static Specification<Product> priceAtMost(BigDecimal max) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("price"), max);
    }

    static Specification<Product> inStock() {
        return (root, query, cb) -> cb.greaterThan(root.get("stock"), 0);
    }

    /** Case-insensitive "contains"; % and _ typed by the user are matched literally, not as wildcards. */
    static Specification<Product> nameContains(String text) {
        String escaped = text.toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return (root, query, cb) -> cb.like(cb.lower(root.get("name")), "%" + escaped + "%", '\\');
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
