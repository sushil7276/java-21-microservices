package com.ecommerce.shipping.client;

import java.util.Map;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import com.ecommerce.shipping.exception.ConflictException;
import com.ecommerce.shipping.resilience.ResilientCaller;

/** shipping-service ===> order-service : "the parcel was shipped / delivered, update the order". */
@Component
public class OrderClient {

    private final RestClient http;
    private final ResilientCaller caller;

    public OrderClient(@LoadBalanced RestClient.Builder builder, ResilientCaller caller) {
        this.http = builder.baseUrl("http://order-service").build();
        this.caller = caller;
    }

    public void updateOrderStatus(Long orderId, String status) {
        caller.run("order-service", "set order " + orderId + " to " + status, () -> {
            try {
                http.put().uri("/internal/orders/{id}/status", orderId)
                        .body(Map.of("status", status)).retrieve().toBodilessEntity();
            } catch (HttpClientErrorException.Conflict e) {
                // e.g. "DELIVERED" before the order was "SHIPPED"
                throw new ConflictException("Order " + orderId + " cannot move to " + status + ": " + e.getResponseBodyAsString());
            }
        });
    }
}
