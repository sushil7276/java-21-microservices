package com.ecommerce.order.controller;

import java.time.Instant;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.order.exception.ForbiddenException;
import com.ecommerce.order.web.Headers;
import com.ecommerce.order.entity.OrderStatus;
import com.ecommerce.order.dto.Dtos.CheckoutRequest;
import com.ecommerce.order.dto.Dtos.OrderResponse;
import com.ecommerce.order.dto.Dtos.OrdersPage;
import com.ecommerce.order.dto.Dtos.UpdateStatusRequest;
import com.ecommerce.order.service.OrderService;

import jakarta.validation.Valid;

@RestController
public class OrderController {

    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    /** 201 when paid, 402 when the payment was declined. */
    @PostMapping("/api/orders/checkout")
    public ResponseEntity<OrderResponse> checkout(@RequestHeader(Headers.USER_ID) Long userId,
                                                  @Valid @RequestBody CheckoutRequest request) {
        OrderResponse order = service.checkout(userId, request);
        HttpStatus status = order.status() == OrderStatus.PAYMENT_FAILED ? HttpStatus.PAYMENT_REQUIRED : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(order);
    }

    @GetMapping("/api/orders")
    public List<OrderResponse> myOrders(@RequestHeader(Headers.USER_ID) Long userId) {
        return service.listForUser(userId);
    }

    @GetMapping("/api/orders/all")
    public List<OrderResponse> all(@RequestHeader(Headers.USER_ROLE) String role) {
        if (!"ADMIN".equals(role)) {
            throw new ForbiddenException("Admin only");
        }
        return service.listAll();
    }

    @GetMapping("/api/orders/{id}")
    public OrderResponse get(@PathVariable Long id,
                             @RequestHeader(Headers.USER_ID) Long userId,
                             @RequestHeader(Headers.USER_ROLE) String role) {
        return service.get(id, userId, role);
    }

    /** Service-to-service: shipping-service reports SHIPPED / DELIVERED. */
    /** Called by batch-service (daily sales report): one page of paid orders created in [from, to). */
    @GetMapping("/internal/orders/sales")
    public OrdersPage sales(@RequestParam Instant from, @RequestParam Instant to,
                            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size) {
        return service.salesPage(from, to, page, size);
    }

    @PutMapping("/internal/orders/{id}/status")
    public OrderResponse updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateStatusRequest request) {
        return service.updateStatus(id, request.status());
    }
}
