package com.ecommerce.batch.resilience;

import java.time.Duration;

import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.HttpClientErrorException;

import com.ecommerce.batch.exception.BadRequestException;
import com.ecommerce.batch.exception.ConflictException;
import com.ecommerce.batch.exception.ForbiddenException;
import com.ecommerce.batch.exception.NotFoundException;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;

/**
 * THE circuit breaker settings, in one place. Every service-to-service call uses these numbers.
 *
 * How a circuit breaker works (like a fuse in your house):
 *   CLOSED    -> normal. Calls go through. We count the last 10 calls.
 *   OPEN      -> when 50% of the last 10 calls failed (and at least 5 calls were made) the fuse "trips".
 *                Calls are rejected IMMEDIATELY (no waiting for a dead service) and the fallback runs.
 *   HALF_OPEN -> after 10 seconds we let 2 test calls through. If they work -> CLOSED again, if not -> OPEN.
 */
@Configuration(proxyBeanMethods = false)
public class ResilienceConfig {

    @Bean
    Customizer<Resilience4JCircuitBreakerFactory> circuitBreakerSettings() {
        CircuitBreakerConfig breaker = CircuitBreakerConfig.custom()
                .slidingWindowSize(10)                                  // look at the last 10 calls
                .minimumNumberOfCalls(5)                                // ...but only judge after 5 calls
                .failureRateThreshold(50)                               // 50% failures -> OPEN
                .waitDurationInOpenState(Duration.ofSeconds(10))        // stay OPEN for 10 s
                .permittedNumberOfCallsInHalfOpenState(2)               // then try 2 calls
                // These are NOT outages - the other service answered correctly ("404 not found" etc.).
                // They must not make the circuit open.
                .ignoreExceptions(NotFoundException.class, BadRequestException.class,
                        ConflictException.class, ForbiddenException.class, HttpClientErrorException.class)
                .build();

        return factory -> factory.configureDefault(id -> new Resilience4JConfigBuilder(id)
                .circuitBreakerConfig(breaker)
                .build());
    }
}
