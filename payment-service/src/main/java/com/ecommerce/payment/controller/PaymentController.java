package com.ecommerce.payment.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.payment.exception.ForbiddenException;
import com.ecommerce.payment.web.Headers;
import com.ecommerce.payment.dto.PaymentResponse;
import com.ecommerce.payment.dto.PaymentRequest;
import com.ecommerce.payment.service.PaymentService;

import jakarta.validation.Valid;

@RestController
public class PaymentController {

    private final PaymentService service;

    public PaymentController(PaymentService service) {
        this.service = service;
    }

    @GetMapping("/api/payments")
    public List<PaymentResponse> mine(@RequestHeader(Headers.USER_ID) Long userId) {
        return service.forUser(userId);
    }

    @GetMapping("/api/payments/order/{orderId}")
    public List<PaymentResponse> forOrder(@PathVariable Long orderId,
                                  @RequestHeader(Headers.USER_ID) Long userId,
                                  @RequestHeader(Headers.USER_ROLE) String role) {
        List<PaymentResponse> payments = service.forOrder(orderId);
        if (!"ADMIN".equals(role) && payments.stream().anyMatch(p -> !p.userId().equals(userId))) {
            throw new ForbiddenException("This payment belongs to another user");
        }
        return payments;
    }

    /** Called by order-service during checkout. */
    @PostMapping("/internal/payments")
    public PaymentResponse pay(@Valid @RequestBody PaymentRequest request) {
        return service.process(request);
    }
}
