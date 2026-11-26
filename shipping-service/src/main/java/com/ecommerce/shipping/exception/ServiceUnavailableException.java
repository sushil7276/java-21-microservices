package com.ecommerce.shipping.exception;

/** Thrown when another microservice is down / too slow / its circuit breaker is OPEN. Becomes HTTP 503. */
public class ServiceUnavailableException extends RuntimeException {
    public ServiceUnavailableException(String message) {
        super(message);
    }
}
