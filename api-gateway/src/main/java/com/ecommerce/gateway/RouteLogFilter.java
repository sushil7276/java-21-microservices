package com.ecommerce.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

/**
 * Writes one line when a request enters the gateway and says WHICH SERVICE it is sent to,
 * and one line when the answer leaves:
 *      [api-gateway] <=== POST /api/orders/checkout  ==> order-service (user 5)
 *      [api-gateway] ---> 201 POST /api/orders/checkout (93 ms)
 */
@Component
public class RouteLogFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(RouteLogFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        String target = route == null ? "no route" : route.getUri().getHost();
        String user = exchange.getRequest().getHeaders().getFirst("X-User-Id");
        String what = exchange.getRequest().getMethod() + " " + exchange.getRequest().getURI().getPath();
        long start = System.nanoTime();

        log.info("[api-gateway] <=== {}  ==> {}{}", what, target, user == null ? "" : " (user " + user + ")");
        return chain.filter(exchange).doFinally(signal -> log.info("[api-gateway] ---> {} {} ({} ms)",
                exchange.getResponse().getStatusCode() == null ? "?" : exchange.getResponse().getStatusCode().value(),
                what, (System.nanoTime() - start) / 1_000_000));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 20;     // after UserHeaderFilter, so X-User-Id is already set
    }
}
