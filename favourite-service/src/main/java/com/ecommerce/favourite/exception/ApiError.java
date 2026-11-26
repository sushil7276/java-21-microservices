package com.ecommerce.favourite.exception;

import java.time.Instant;

public record ApiError(Instant timestamp, int status, String error, String message, Object details) {
    public static ApiError of(int status, String error, String message, Object details) {
        return new ApiError(Instant.now(), status, error, message, details);
    }
}
