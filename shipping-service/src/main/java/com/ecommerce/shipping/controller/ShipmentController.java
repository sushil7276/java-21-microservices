package com.ecommerce.shipping.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.shipping.dto.ShipmentResponse;
import com.ecommerce.shipping.dto.Dtos.CreateShipmentRequest;
import com.ecommerce.shipping.dto.Dtos.UpdateStatusRequest;
import com.ecommerce.shipping.service.ShipmentService;

import jakarta.validation.Valid;

@RestController
public class ShipmentController {

    private final ShipmentService service;

    public ShipmentController(ShipmentService service) {
        this.service = service;
    }

    @GetMapping("/api/shipments/order/{orderId}")
    public ShipmentResponse byOrder(@PathVariable Long orderId) {
        return service.byOrder(orderId);
    }

    @GetMapping("/api/shipments/track/{trackingNumber}")
    public ShipmentResponse track(@PathVariable String trackingNumber) {
        return service.byTracking(trackingNumber);
    }

    /** ADMIN only (enforced at the gateway) - simulates the courier updating the parcel. */
    @PutMapping("/api/shipments/{id}/status")
    public ShipmentResponse updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateStatusRequest request) {
        return service.updateStatus(id, request.status());
    }

    /** Called by order-service after a successful payment. */
    @PostMapping("/internal/shipments")
    public ShipmentResponse create(@Valid @RequestBody CreateShipmentRequest request) {
        return service.create(request);
    }
}
