package com.ecommerce.order.client;

import java.math.BigDecimal;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.ecommerce.order.resilience.ResilientCaller;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** order-service ===> payment-service : charge the customer. */
@Component
public class PaymentClient {

    public record PaymentRequest(Long orderId, Long userId, BigDecimal amount, String method) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PaymentResult(Long id, Long orderId, String status, String transactionId, String message) {
        public boolean isSuccess() {
            return "SUCCESS".equals(status);
        }
    }

    private final RestClient http;
    private final ResilientCaller caller;

    public PaymentClient(@LoadBalanced RestClient.Builder builder, ResilientCaller caller) {
        this.http = builder.baseUrl("http://payment-service").build();
        this.caller = caller;
    }

    public PaymentResult pay(PaymentRequest request) {
        return caller.call("payment-service", "charge order " + request.orderId(),
                () -> http.post().uri("/internal/payments").body(request).retrieve().body(PaymentResult.class));
    }
}
