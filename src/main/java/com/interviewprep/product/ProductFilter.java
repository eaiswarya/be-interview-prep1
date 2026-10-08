package com.interviewprep.product;

import java.math.BigDecimal;

/** Optional list filters; null means "not filtered". */
public record ProductFilter(String category, BigDecimal minPrice, BigDecimal maxPrice, Boolean inStock, String q) {
}
