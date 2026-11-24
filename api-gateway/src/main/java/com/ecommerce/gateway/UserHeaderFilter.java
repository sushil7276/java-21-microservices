package com.ecommerce.gateway;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Strips any identity headers sent by the client (anti-spoofing) and replaces them with
 * values taken from the verified JWT, so downstream services can trust them.
 */
@Component
public class UserHeaderFilter implements GlobalFilter, Ordered {

    static final String USER_ID = "X-User-Id";
    static final String USER_NAME = "X-User-Name";
    static final String USER_ROLE = "X-User-Role";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest cleaned = exchange.getRequest().mutate()
                .headers(h -> {
                    h.remove(USER_ID);
                    h.remove(USER_NAME);
                    h.remove(USER_ROLE);
                }).build();

        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .filter(a -> a instanceof JwtAuthenticationToken)
                .cast(JwtAuthenticationToken.class)
                .map(auth -> {
                    Jwt jwt = auth.getToken();
                    return cleaned.mutate()
                            .header(USER_ID, String.valueOf(jwt.getClaims().get("uid")))
                            .header(USER_NAME, jwt.getSubject())
                            .header(USER_ROLE, String.valueOf(jwt.getClaims().get("role")))
                            .build();
                })
                .defaultIfEmpty(cleaned)
                .flatMap(req -> chain.filter(exchange.mutate().request(req).build()));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}
