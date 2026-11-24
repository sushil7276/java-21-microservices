package com.ecommerce.order.client;


import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.ecommerce.common.resilience.ResilientCaller;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** order-service ===> shipping-service : create the delivery. */
@Component
public class ShippingClient {

    public record ShipmentRequest(Long orderId, Long userId, String address) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ShipmentResult(Long id, Long orderId, String trackingNumber, String status) {
    }

    private final RestClient http;
    private final ResilientCaller caller;

    public ShippingClient(@LoadBalanced RestClient.Builder builder, ResilientCaller caller) {
        this.http = builder.baseUrl("http://shipping-service").build();
        this.caller = caller;
    }

    public ShipmentResult createShipment(ShipmentRequest request) {
        return caller.call("shipping-service", "create shipment for order " + request.orderId(),
                () -> http.post().uri("/internal/shipments").body(request).retrieve().body(ShipmentResult.class));
    }
}
