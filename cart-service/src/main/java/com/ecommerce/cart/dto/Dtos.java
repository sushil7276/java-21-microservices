package com.ecommerce.cart.dto;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public final class Dtos {
    private Dtos() {
    }

    public record AddItemRequest(@NotNull Long productId, @NotNull @Min(1) Integer quantity) {
    }

    public record CartItemResponse(Long productId, String productName, BigDecimal unitPrice, Integer quantity,
                                   BigDecimal lineTotal) {
    }

    public record CartResponse(Long userId, List<CartItemResponse> items, BigDecimal total) {
    }
}
