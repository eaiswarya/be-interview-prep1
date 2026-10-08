package com.interviewprep.product;

import com.interviewprep.common.PageResponse;
import java.math.BigDecimal;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    /**
     * Example: /api/products?category=Books&minPrice=10&maxPrice=50&inStock=true&q=lamp&page=0&size=20&sort=price,desc
     * The page size is capped at 100 by spring.data.web.pageable.max-page-size; a default sort by id keeps
     * pages stable.
     */
    @GetMapping
    public PageResponse<ProductResponse> list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Boolean inStock,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return service.list(new ProductFilter(category, minPrice, maxPrice, inStock, q), pageable);
    }
}
