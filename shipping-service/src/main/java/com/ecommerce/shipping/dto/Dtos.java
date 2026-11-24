package com.ecommerce.shipping.dto;

import com.ecommerce.shipping.entity.ShipmentStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public final class Dtos {
    private Dtos() {
    }

    public record CreateShipmentRequest(@NotNull Long orderId, @NotNull Long userId, @NotBlank String address) {
    }

    public record UpdateStatusRequest(@NotNull ShipmentStatus status) {
    }
}
