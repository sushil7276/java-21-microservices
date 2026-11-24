package com.ecommerce.order.client;


import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import com.ecommerce.common.exception.ConflictException;
import com.ecommerce.common.exception.NotFoundException;
import com.ecommerce.common.resilience.ResilientCaller;

/** order-service ===> product-service : take stock out / put it back. */
@Component
public class ProductClient {

    private final RestClient http;
    private final ResilientCaller caller;

    public ProductClient(@LoadBalanced RestClient.Builder builder, ResilientCaller caller) {
        this.http = builder.baseUrl("http://product-service").build();
        this.caller = caller;
    }

    public void reduceStock(Long productId, int quantity) {
        caller.run("product-service", "reduce stock of product " + productId + " by " + quantity, () -> {
            try {
                http.post().uri("/internal/products/{id}/reduce-stock?quantity={q}", productId, quantity)
                        .retrieve().toBodilessEntity();
            } catch (HttpClientErrorException.NotFound e) {
                throw new NotFoundException("Product not found: " + productId);
            } catch (HttpClientErrorException.Conflict e) {
                throw new ConflictException("Insufficient stock for product " + productId);
            }
        });
    }

    public void restoreStock(Long productId, int quantity) {
        caller.run("product-service", "give back " + quantity + " x product " + productId,
                () -> http.post().uri("/internal/products/{id}/restore-stock?quantity={q}", productId, quantity)
                        .retrieve().toBodilessEntity());
    }
}
