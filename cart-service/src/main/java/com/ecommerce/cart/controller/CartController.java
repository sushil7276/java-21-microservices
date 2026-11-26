package com.ecommerce.cart.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.cart.dto.Dtos.AddItemRequest;
import com.ecommerce.cart.dto.Dtos.CartResponse;
import com.ecommerce.cart.service.CartService;
import com.ecommerce.cart.web.Headers;

import jakarta.validation.Valid;

@RestController
public class CartController {

    private final CartService service;

    public CartController(CartService service) {
        this.service = service;
    }

    // ---- customer endpoints: the cart owner comes from the verified JWT (X-User-Id header) ----

    @GetMapping("/api/cart")
    public CartResponse get(@RequestHeader(Headers.USER_ID) Long userId) {
        return service.get(userId);
    }

    @PostMapping("/api/cart/items")
    @ResponseStatus(HttpStatus.CREATED)
    public CartResponse add(@RequestHeader(Headers.USER_ID) Long userId, @Valid @RequestBody AddItemRequest request) {
        return service.addItem(userId, request);
    }

    @DeleteMapping("/api/cart/items/{productId}")
    public CartResponse remove(@RequestHeader(Headers.USER_ID) Long userId, @PathVariable Long productId) {
        return service.removeItem(userId, productId);
    }

    @DeleteMapping("/api/cart")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearMine(@RequestHeader(Headers.USER_ID) Long userId) {
        service.clear(userId);
    }

    // ---- internal endpoints used by order-service ----

    @GetMapping("/internal/carts/{userId}")
    public CartResponse internalGet(@PathVariable Long userId) {
        return service.get(userId);
    }

    /** Called by batch-service (clean-up job): deletes carts that were not changed for {@code days} days. */
    @DeleteMapping("/internal/carts/abandoned")
    public Map<String, Integer> internalDeleteAbandoned(@RequestParam(defaultValue = "14") int days) {
        return Map.of("deleted", service.deleteAbandoned(days));
    }

    @DeleteMapping("/internal/carts/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void internalClear(@PathVariable Long userId) {
        service.clear(userId);
    }
}
