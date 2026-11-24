package com.ecommerce.payment.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.ecommerce.payment.entity.Payment;
import com.ecommerce.payment.dto.PaymentRequest;
import com.ecommerce.payment.dto.PaymentResponse;
import com.ecommerce.payment.mapper.PaymentMapper;
import com.ecommerce.payment.repository.PaymentRepository;

/**
 * Simulated payment gateway.
 * - method "CARD_DECLINED"  -> always fails (use it to test the failure path)
 * - amount above 100000     -> fails (limit exceeded)
 * - everything else         -> succeeds
 */
@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private final PaymentRepository repository;
    private final PaymentMapper mapper;

    public PaymentService(PaymentRepository repository, PaymentMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    public PaymentResponse process(PaymentRequest req) {
        // idempotency: never charge the same order twice
        var existing = repository.findFirstByOrderIdAndStatus(req.orderId(), "SUCCESS");
        if (existing.isPresent()) {
            return mapper.toResponse(existing.get());
        }
        boolean declined = "CARD_DECLINED".equalsIgnoreCase(req.method());
        boolean overLimit = req.amount().compareTo(new java.math.BigDecimal("100000")) > 0;
        boolean success = !declined && !overLimit;

        Payment payment = repository.save(Payment.builder()
                .orderId(req.orderId()).userId(req.userId()).amount(req.amount()).method(req.method())
                .status(success ? "SUCCESS" : "FAILED")
                .transactionId(success ? "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase() : null)
                .message(success ? "Payment approved" : declined ? "Card declined" : "Amount exceeds limit")
                .createdAt(Instant.now())
                .build());
        log.info("Payment for order {} -> {}", req.orderId(), payment.getStatus());
        return mapper.toResponse(payment);
    }

    public List<PaymentResponse> forUser(Long userId) {
        return mapper.toResponses(repository.findByUserIdOrderByCreatedAtDesc(userId));
    }

    public List<PaymentResponse> forOrder(Long orderId) {
        return mapper.toResponses(repository.findByOrderId(orderId));
    }
}
