package com.ecommerce.shipping.dto;

import java.time.Instant;

import com.ecommerce.shipping.entity.ShipmentStatus;

public record ShipmentResponse(Long id, Long orderId, Long userId, String address, String trackingNumber,
                               ShipmentStatus status, Instant createdAt, Instant updatedAt) {
}
