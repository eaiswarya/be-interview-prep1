package com.interviewprep.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Entity
public class ShortLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The unique constraint is the real uniqueness guarantee; the generator only makes collisions unlikely.
    @Column(nullable = false, unique = true, length = 8)
    private String code;

    @Column(nullable = false, length = 2048)
    private String originalUrl;

    private Instant expiresAt;

    // Only ever changed by the atomic UPDATE in ShortLinkRepository, never through this entity.
    @Column(nullable = false)
    private long visitCount;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected ShortLink() {
    }

    public ShortLink(String code, String originalUrl, Instant expiresAt) {
        this.code = code;
        this.originalUrl = originalUrl;
        this.expiresAt = expiresAt;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now().truncatedTo(ChronoUnit.MILLIS);
    }

    public boolean isExpired(Instant now) {
        return expiresAt != null && !now.isBefore(expiresAt);
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public long getVisitCount() {
        return visitCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
