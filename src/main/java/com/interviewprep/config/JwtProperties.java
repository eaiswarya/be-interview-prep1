package com.interviewprep.config;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Bound from app.jwt.*. The secret has no default in source: it comes from the JWT_SECRET environment
 * variable, and the app refuses to start without it.
 */
@ConfigurationProperties("app.jwt")
public record JwtProperties(String secret, Duration ttl, String issuer) {

    /** HS256 needs a key of at least 256 bits. */
    static final int MIN_SECRET_BYTES = 32;

    public JwtProperties {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "app.jwt.secret (env JWT_SECRET) must be at least " + MIN_SECRET_BYTES + " bytes");
        }
    }

    public SecretKey signingKey() {
        return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }
}
