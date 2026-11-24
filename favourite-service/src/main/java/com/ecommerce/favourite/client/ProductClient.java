package com.ecommerce.favourite.client;

import java.math.BigDecimal;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import com.ecommerce.common.exception.NotFoundException;
import com.ecommerce.common.resilience.ResilientCaller;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** favourite-service ===> product-service : show the product details of a favourite. */
@Component
public class ProductClient {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ProductDto(Long id, String name, String description, BigDecimal price, Integer stock, String category) {
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
