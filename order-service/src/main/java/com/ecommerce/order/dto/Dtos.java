package com.ecommerce.order.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.ecommerce.order.entity.OrderStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public final class Dtos {
    private Dtos() {
    }

    public record CheckoutRequest(@NotBlank String shippingAddress, @NotBlank String paymentMethod) {
    }

    public record UpdateStatusRequest(@NotNull OrderStatus status) {
    }

    public record OrderItemResponse(Long productId, String productName, BigDecimal unitPrice, Integer quantity) {
    }

    /** One page of orders for the sales report. {@code last} is true when there is no further page. */
    public record OrdersPage(List<OrderResponse> content, boolean last) {
    }

    public record OrderResponse(Long id, Long userId, String customerName, OrderStatus status,
                                BigDecimal totalAmount, String shippingAddress, String paymentMethod,
                                String paymentTransactionId, String trackingNumber, String failureReason,
                                Instant createdAt, Instant updatedAt, List<OrderItemResponse> items) {
    }
}
