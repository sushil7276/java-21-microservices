package com.ecommerce.gateway;

import java.net.ConnectException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.TimeoutException;

import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;

import reactor.core.publisher.Mono;

/**
 * When a service is not reachable at all ("connection refused") or too slow, the gateway answers with a
 * clean JSON 503 instead of a raw 500. (Once the circuit breaker is OPEN, FallbackController answers.)
 */
@Component
@Order(-2)          // runs before Spring Boot's default error handler
public class GatewayErrorHandler implements WebExceptionHandler {

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable error) {
        if (exchange.getResponse().isCommitted() || !isUnreachable(error)) {
            return Mono.error(error);                                  // not ours - let Spring handle it
        }
        Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        String service = route == null ? "the service" : route.getUri().getHost();

        String json = "{\"timestamp\":\"" + Instant.now() + "\",\"status\":503,\"error\":\"Service Unavailable\","
                + "\"message\":\"" + service + " is currently unavailable. Please try again later.\"}";
        exchange.getResponse().setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        return exchange.getResponse().writeWith(Mono.just(
                exchange.getResponse().bufferFactory().wrap(json.getBytes(StandardCharsets.UTF_8))));
    }

    private boolean isUnreachable(Throwable error) {
        for (Throwable t = error; t != null; t = t.getCause()) {
            if (t instanceof ConnectException || t instanceof TimeoutException) {
                return true;
            }
        }
        return false;
    }
}
