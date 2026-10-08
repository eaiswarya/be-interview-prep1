package com.interviewprep.service;

import com.interviewprep.config.CacheConfig;
import com.interviewprep.dto.PageResponse;
import com.interviewprep.dto.ProductFilter;
import com.interviewprep.dto.ProductRequest;
import com.interviewprep.dto.ProductResponse;
import com.interviewprep.exception.InvalidRequestException;
import com.interviewprep.exception.ResourceNotFoundException;
import com.interviewprep.model.Product;
import com.interviewprep.repository.ProductRepository;
import com.interviewprep.repository.ProductSpecifications;
import java.util.Set;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
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

    /**
     * Cached by id. sync = true makes concurrent misses for the same id wait for a single database load
     * (Caffeine computes the entry atomically), and an eviction issued while that load is in flight waits for
     * it and then removes it, so an old row loaded just before an update cannot survive in the cache.
     */
    @Cacheable(cacheNames = CacheConfig.PRODUCTS, key = "#id", sync = true)
    @Transactional(readOnly = true)
    public ProductResponse get(Long id) {
        return ProductResponse.from(find(id));
    }

    /** Evicts rather than puts: the next read reloads the committed row, so the cache never holds uncommitted data. */
    @CacheEvict(cacheNames = CacheConfig.PRODUCTS, key = "#id")
    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = find(id);
        product.update(request.name(), request.category(), request.price(), request.stock(), request.rating());
        return ProductResponse.from(product);
    }

    @CacheEvict(cacheNames = CacheConfig.PRODUCTS, key = "#id")
    @Transactional
    public void delete(Long id) {
        repository.delete(find(id));
    }

    private Product find(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Product", id));
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
