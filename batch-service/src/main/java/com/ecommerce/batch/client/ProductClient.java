package com.ecommerce.batch.client;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.ecommerce.batch.resilience.ResilientCaller;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** batch-service ===> product-service. "http://product-service" has no port: Eureka says where it runs. */
@Component
public class ProductClient {

    /** What product-service expects for a new product (the same fields as its ProductRequest). */
    public record NewProduct(String name, String description, BigDecimal price, Integer stock, String category,
                             String imageUrl) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record BulkResult(int created, List<Rejected> rejected) {
        /** @param index position in the list that was sent (0 = first) */
        @JsonIgnoreProperties(ignoreUnknown = true)
        public record Rejected(int index, String name, String reason) {
        }
    }

    private final RestClient http;
    private final ResilientCaller caller;

    public ProductClient(@LoadBalanced RestClient.Builder builder, ResilientCaller caller) {
        this.http = builder.baseUrl("http://product-service").build();
        this.caller = caller;
    }

    /** Creates a whole chunk of products with ONE call (internal endpoint, not reachable from outside). */
    public BulkResult bulkCreate(List<NewProduct> products) {
        return caller.call("product-service", "bulk create " + products.size() + " products",
                () -> http.post().uri("/internal/products/bulk").body(products).retrieve()
                        .body(new ParameterizedTypeReference<BulkResult>() { }));
    }
}
