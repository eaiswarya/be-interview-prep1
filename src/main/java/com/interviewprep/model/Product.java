package com.interviewprep.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

// Indexes on the columns used to filter and sort, so listing stays fast as the catalog grows.
@Entity
@Table(indexes = {
        @Index(name = "idx_product_category", columnList = "category"),
        @Index(name = "idx_product_price", columnList = "price"),
        @Index(name = "idx_product_name", columnList = "name")
})
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 50)
    private String category;

    // BigDecimal, not double: money must not pick up binary rounding errors.
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private int stock;

    @Column(nullable = false)
    private double rating;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected Product() {
    }

    public Product(String name, String category, BigDecimal price, int stock, double rating) {
        update(name, category, price, stock, rating);
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now().truncatedTo(ChronoUnit.MILLIS);
    }

    public void update(String name, String category, BigDecimal price, int stock, double rating) {
        this.name = name;
        this.category = category;
        this.price = price;
        this.stock = stock;
        this.rating = rating;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public int getStock() {
        return stock;
    }

    public double getRating() {
        return rating;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
