package com.ecommerce.cart.client;

import java.math.BigDecimal;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import com.ecommerce.cart.exception.NotFoundException;
import com.ecommerce.cart.resilience.ResilientCaller;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * cart-service  ===>  product-service
 * "http://product-service" has no port: Eureka tells us where product-service runs.
 */
@Component
public class ProductClient {

    /** The few fields of a product that the cart needs. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ProductDto(Long id, String name, BigDecimal price, Integer stock) {
    }

    private final RestClient http;
    private final ResilientCaller caller;

    public ProductClient(@LoadBalanced RestClient.Builder builder, ResilientCaller caller) {
        this.http = builder.baseUrl("http://product-service").build();
        this.caller = caller;
    }

    public ProductDto getProduct(Long productId) {
        return caller.call("product-service", "get product " + productId, () -> {
            try {
                return http.get().uri("/api/products/{id}", productId).retrieve().body(ProductDto.class);
            } catch (HttpClientErrorException.NotFound e) {
                throw new NotFoundException("Product not found: " + productId);
            }
        });
    }
}
