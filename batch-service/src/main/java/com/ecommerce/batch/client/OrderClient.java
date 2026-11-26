package com.ecommerce.batch.client;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.ecommerce.batch.resilience.ResilientCaller;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** batch-service ===> order-service: reads the paid orders of a time range, one page at a time. */
@Component
public class OrderClient {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OrderItemDto(Long productId, String productName, BigDecimal unitPrice, Integer quantity) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OrderDto(Long id, BigDecimal totalAmount, List<OrderItemDto> items) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OrdersPage(List<OrderDto> content, boolean last) {
    }

    private final RestClient http;
    private final ResilientCaller caller;

    public OrderClient(@LoadBalanced RestClient.Builder builder, ResilientCaller caller) {
        this.http = builder.baseUrl("http://order-service").build();
        this.caller = caller;
    }

    /** Orders created in [from, to) that count as sales (paid, shipped or delivered). */
    public OrdersPage salesPage(Instant from, Instant to, int page, int size) {
        return caller.call("order-service", "sales page " + page, () -> http.get()
                .uri("/internal/orders/sales?from={from}&to={to}&page={page}&size={size}", from, to, page, size)
                .retrieve().body(OrdersPage.class));
    }
}
