package com.interviewprep.dto;

import com.interviewprep.model.ShortLink;
import java.time.Instant;

public record ShortLinkResponse(
        String code,
        String shortUrl,
        String originalUrl,
        Instant expiresAt,
        Instant createdAt) {

    public static ShortLinkResponse from(ShortLink link, String shortUrl) {
        return new ShortLinkResponse(
                link.getCode(), shortUrl, link.getOriginalUrl(), link.getExpiresAt(), link.getCreatedAt());
    }
}
