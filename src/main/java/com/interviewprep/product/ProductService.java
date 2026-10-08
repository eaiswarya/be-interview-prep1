package com.interviewprep.product;

import com.interviewprep.common.InvalidRequestException;
import com.interviewprep.common.PageResponse;
import java.util.Set;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

    static final Set<String> SORTABLE_FIELDS = Set.of("id", "name", "category", "price", "stock", "rating", "createdAt");

    private final ProductRepository repository;

    public ProductService(ProductRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> list(ProductFilter filter, Pageable pageable) {
        validate(filter, pageable.getSort());
        return PageResponse.from(
                repository.findAll(ProductSpecifications.matching(filter), pageable).map(ProductResponse::from));
    }

    private static void validate(ProductFilter filter, Sort sort) {
        // Unknown sort fields would otherwise fail deep inside JPA as a 500.
        for (Sort.Order order : sort) {
            if (!SORTABLE_FIELDS.contains(order.getProperty())) {
                throw new InvalidRequestException("sort",
                        "cannot sort by '" + order.getProperty() + "'; allowed: " + SORTABLE_FIELDS);
            }
        }
        if (filter.minPrice() != null && filter.minPrice().signum() < 0) {
            throw new InvalidRequestException("minPrice", "must not be negative");
        }
        if (filter.minPrice() != null && filter.maxPrice() != null
                && filter.minPrice().compareTo(filter.maxPrice()) > 0) {
            throw new InvalidRequestException("minPrice", "must not be greater than maxPrice");
        }
    }
}
