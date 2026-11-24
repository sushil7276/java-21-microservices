package com.ecommerce.payment.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PaymentRequest(@NotNull Long orderId, @NotNull Long userId,
                             @NotNull @DecimalMin("0.01") BigDecimal amount, @NotBlank String method) {
}
