package com.ecommerce.shipping.resilience;

import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

import com.ecommerce.shipping.exception.BadRequestException;
import com.ecommerce.shipping.exception.ConflictException;
import com.ecommerce.shipping.exception.ForbiddenException;
import com.ecommerce.shipping.exception.NotFoundException;
import com.ecommerce.shipping.exception.ServiceUnavailableException;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;

/**
 * Every call from one microservice to another goes through this class:
 *
 *      caller.call("product-service", "reduce stock", () -> restClient.post()...);
 *
 * It (1) logs "who calls whom", (2) wraps the call in a circuit breaker named after the target service
 * (one breaker per target service), (3) turns "service is down" into a clean HTTP 503.
 */
@Component
public class ResilientCaller {

    private static final Logger log = LoggerFactory.getLogger(ResilientCaller.class);

    private final CircuitBreakerFactory<?, ?> circuitBreakerFactory;
    private final String me;

    public ResilientCaller(CircuitBreakerFactory<?, ?> circuitBreakerFactory,
                           @Value("${spring.application.name}") String me) {
        this.circuitBreakerFactory = circuitBreakerFactory;
        this.me = me;
    }

    /** Call another service and return its answer. */
    public <T> T call(String targetService, String what, Supplier<T> action) {
        log.info("[{}] ===> calling {} : {}", me, targetService, what);
        return circuitBreakerFactory.create(targetService)
                .run(action, error -> fallback(targetService, what, error));
    }

    /** Same, for calls that return nothing. */
    public void run(String targetService, String what, Runnable action) {
        call(targetService, what, () -> {
            action.run();
            return null;
        });
    }

    /** Runs when the call failed OR the circuit is open. */
    private <T> T fallback(String targetService, String what, Throwable error) {
        // The other service ANSWERED with a business error (404, 409 ...): pass it on unchanged.
        if (error instanceof NotFoundException || error instanceof BadRequestException
                || error instanceof ConflictException || error instanceof ForbiddenException
                || error instanceof HttpClientErrorException) {
            throw (RuntimeException) error;
        }
        String reason = error instanceof CallNotPermittedException
                ? "circuit breaker is OPEN (call rejected without trying)"
                : error.getClass().getSimpleName() + ": " + error.getMessage();
        log.warn("[{}] XXX {} failed while '{}' -> {}. Returning 503.", me, targetService, what, reason);
        throw new ServiceUnavailableException(targetService + " is currently unavailable. Please try again later.");
    }
}
