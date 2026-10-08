package com.interviewprep.dto;

import java.util.List;
import org.springframework.data.domain.Page;

/**
 * A stable JSON shape for paged results. Serialising Spring's Page directly exposes internal fields
 * that can change between versions.
 */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
