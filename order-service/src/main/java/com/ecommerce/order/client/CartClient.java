package com.ecommerce.order.client;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.ecommerce.order.resilience.ResilientCaller;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** order-service ===> cart-service : what is in the cart? (and empty it after the purchase) */
@Component
public class CartClient {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CartItemDto(Long productId, String productName, BigDecimal unitPrice, Integer quantity) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CartDto(Long userId, List<CartItemDto> items, BigDecimal total) {
    }

    private final RestClient http;
    private final ResilientCaller caller;

    public CartClient(@LoadBalanced RestClient.Builder builder, ResilientCaller caller) {
        this.http = builder.baseUrl("http://cart-service").build();
        this.caller = caller;
    }

    public CartDto getCart(Long userId) {
        return caller.call("cart-service", "read cart of user " + userId,
                () -> http.get().uri("/internal/carts/{id}", userId).retrieve().body(CartDto.class));
    }

    public void clearCart(Long userId) {
        caller.run("cart-service", "empty cart of user " + userId,
                () -> http.delete().uri("/internal/carts/{id}", userId).retrieve().toBodilessEntity());
    }
}
