package com.ecommerce.payment.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentResponse(Long id, Long orderId, Long userId, BigDecimal amount, String method,
                              String status, String transactionId, String message, Instant createdAt) {
}
