package com.ecommerce.gateway;

import java.time.Duration;

import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;

/**
 * Circuit breaker of the gateway: one breaker per route (see application.yml, filter "CircuitBreaker").
 * Same idea as in the services (see ResilienceConfig in cart-service, order-service ...):
 * 50% failures of the last 10 calls -> OPEN for 10 s -> 2 test calls -> CLOSED again.
 * The time limiter gives a service 10 s to answer before the gateway gives up.
 */
@Configuration
public class GatewayResilienceConfig {

    @Bean
    Customizer<ReactiveResilience4JCircuitBreakerFactory> gatewayBreakerSettings() {
        return factory -> factory.configureDefault(id -> new Resilience4JConfigBuilder(id)
                .circuitBreakerConfig(CircuitBreakerConfig.custom()
                        .slidingWindowSize(10)
                        .minimumNumberOfCalls(5)
                        .failureRateThreshold(50)
                        .waitDurationInOpenState(Duration.ofSeconds(10))
                        .permittedNumberOfCallsInHalfOpenState(2)
                        .build())
                .timeLimiterConfig(TimeLimiterConfig.custom().timeoutDuration(Duration.ofSeconds(10)).build())
                .build());
    }
}
