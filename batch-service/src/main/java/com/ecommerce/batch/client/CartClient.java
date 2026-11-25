package com.ecommerce.batch.client;

import java.util.Map;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.ecommerce.common.resilience.ResilientCaller;

/** batch-service ===> cart-service */
@Component
public class CartClient {

    private final RestClient http;
    private final ResilientCaller caller;

    public CartClient(@LoadBalanced RestClient.Builder builder, ResilientCaller caller) {
        this.http = builder.baseUrl("http://cart-service").build();
        this.caller = caller;
    }

    /** Deletes the carts nobody changed for {@code days} days. Returns how many were deleted. */
    public long deleteAbandoned(int days) {
        Map<String, Number> answer = caller.call("cart-service", "delete carts idle for " + days + " days",
                () -> http.delete().uri("/internal/carts/abandoned?days={d}", days).retrieve()
                        .body(new ParameterizedTypeReference<Map<String, Number>>() { }));
        return answer == null ? 0 : answer.getOrDefault("deleted", 0).longValue();
    }
}
