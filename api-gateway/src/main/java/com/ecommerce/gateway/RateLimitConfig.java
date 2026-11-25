package com.ecommerce.gateway;

import java.net.InetSocketAddress;
import java.security.Principal;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.server.reactive.ServerHttpRequest;

import reactor.core.publisher.Mono;

/**
 * REDIS RATE LIMITING: "who is being counted?"
 * --------------------------------------------
 * The limits themselves (requests per second, burst size) are written in application.yml next to every route.
 * A KeyResolver answers the other question: for one request, which Redis counter should be used?
 *
 *   clientIpKeyResolver    one counter per IP address.        Used for login / register (stops password guessing).
 *   userOrIpKeyResolver    one counter per logged-in user, or per IP address when nobody is logged in.
 *
 * Redis keeps the counters, so the limit is shared by all gateway instances and survives a gateway restart.
 * If Redis is down the limiter lets the requests through (the shop keeps working, only the protection is off).
 *
 * Look at the counters:  docker exec ecommerce-redis redis-cli --scan --pattern 'request_rate_limiter*'
 */
@Configuration
public class RateLimitConfig {

    @Bean
    KeyResolver clientIpKeyResolver() {
        return exchange -> Mono.just("ip:" + clientIp(exchange.getRequest()));
    }

    // @Primary = the default resolver. Spring Cloud Gateway insists on one default when there are several;
    // every route in application.yml still names the resolver it wants explicitly.
    @Bean
    @Primary
    KeyResolver userOrIpKeyResolver() {
        // exchange.getPrincipal() is the logged-in user (the JWT was already checked by Spring Security).
        return exchange -> exchange.getPrincipal()
                .map(Principal::getName)
                .map(name -> "user:" + name)
                .switchIfEmpty(Mono.fromSupplier(() -> "ip:" + clientIp(exchange.getRequest())));
    }

    private static String clientIp(ServerHttpRequest request) {
        // Behind a proxy / load balancer the real address is the first entry of X-Forwarded-For.
        String forwarded = request.getHeaders().getFirst("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        InetSocketAddress remote = request.getRemoteAddress();
        return remote == null || remote.getAddress() == null ? "unknown" : remote.getAddress().getHostAddress();
    }
}
