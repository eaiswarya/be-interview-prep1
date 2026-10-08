package com.interviewprep.dto;

import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;

/** The single JSON shape returned for every error. */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldError> fieldErrors) {

    public record FieldError(String field, String message) {
    }

    public static ApiError of(HttpStatus status, String message, String path, List<FieldError> fieldErrors) {
        return new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), message, path, fieldErrors);
    }
}
