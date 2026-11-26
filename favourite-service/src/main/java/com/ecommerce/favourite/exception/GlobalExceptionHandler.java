package com.ecommerce.favourite.exception;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

/** One consistent JSON error format for every microservice. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NotFoundException.class)
    ResponseEntity<ApiError> notFound(NotFoundException e) {
        return build(HttpStatus.NOT_FOUND, e.getMessage(), null);
    }

    @ExceptionHandler(BadRequestException.class)
    ResponseEntity<ApiError> badRequest(BadRequestException e) {
        return build(HttpStatus.BAD_REQUEST, e.getMessage(), null);
    }

    @ExceptionHandler(ConflictException.class)
    ResponseEntity<ApiError> conflict(ConflictException e) {
        return build(HttpStatus.CONFLICT, e.getMessage(), null);
    }

    @ExceptionHandler(ForbiddenException.class)
    ResponseEntity<ApiError> forbidden(ForbiddenException e) {
        return build(HttpStatus.FORBIDDEN, e.getMessage(), null);
    }

    @ExceptionHandler(ServiceUnavailableException.class)
    ResponseEntity<ApiError> unavailable(ServiceUnavailableException e) {
        return build(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> validation(MethodArgumentNotValidException e) {
        Map<String, String> fields = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors().forEach(f -> fields.put(f.getField(), f.getDefaultMessage()));
        return build(HttpStatus.BAD_REQUEST, "Validation failed", fields);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MissingRequestHeaderException.class})
    ResponseEntity<ApiError> malformed(Exception e) {
        return build(HttpStatus.BAD_REQUEST, e.getMessage(), null);
    }

    @ExceptionHandler(RestClientResponseException.class)
    ResponseEntity<ApiError> downstream(RestClientResponseException e) {
        log.warn("Downstream call failed: {} {}", e.getStatusCode(), e.getResponseBodyAsString());
        return build(HttpStatus.BAD_GATEWAY, "Downstream service error: " + e.getStatusCode()
                + " " + e.getResponseBodyAsString(), null);
    }

    @ExceptionHandler(ResourceAccessException.class)
    ResponseEntity<ApiError> unreachable(ResourceAccessException e) {
        log.warn("Downstream service unreachable: {}", e.getMessage());
        return build(HttpStatus.SERVICE_UNAVAILABLE, "A required service is unavailable: " + e.getMessage(), null);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> other(Exception e) {
        // Spring's own web exceptions (404 resource, 405 method, 415 ...) already carry the right status
        if (e instanceof ErrorResponse er) {
            return build(er.getStatusCode(), e.getMessage(), null);
        }
        log.error("Unhandled exception", e);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error: " + e.getMessage(), null);
    }

    private ResponseEntity<ApiError> build(HttpStatusCode status, String message, Object details) {
        String reason = status instanceof HttpStatus hs ? hs.getReasonPhrase() : status.toString();
        return ResponseEntity.status(status).body(ApiError.of(status.value(), reason, message, details));
    }
}
