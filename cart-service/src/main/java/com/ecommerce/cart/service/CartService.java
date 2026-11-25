package com.ecommerce.cart.service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;

import com.ecommerce.cart.client.ProductClient;
import com.ecommerce.cart.client.ProductClient.ProductDto;
import com.ecommerce.cart.dto.Dtos.AddItemRequest;
import com.ecommerce.cart.dto.Dtos.CartItemResponse;
import com.ecommerce.cart.dto.Dtos.CartResponse;
import com.ecommerce.cart.store.CartLine;
import com.ecommerce.cart.store.CartStore;
import com.ecommerce.common.exception.ConflictException;
import com.ecommerce.common.exception.NotFoundException;

/** The cart logic. The carts themselves are kept in Redis (see {@link CartStore}). */
@Service
public class CartService {

    private final CartStore store;
    private final ProductClient productClient;

    public CartService(CartStore store, ProductClient productClient) {
        this.store = store;
        this.productClient = productClient;
    }

    public CartResponse get(Long userId) {
        return toResponse(userId);
    }

    public CartResponse addItem(Long userId, AddItemRequest req) {
        ProductDto product = productClient.getProduct(req.productId());   // price is always taken from product-service

        int oldQuantity = store.item(userId, product.id()).map(CartLine::quantity).orElse(0);
        int newQuantity = oldQuantity + req.quantity();
        if (product.stock() != null && newQuantity > product.stock()) {
            throw new ConflictException("Only " + product.stock() + " units of '" + product.name() + "' in stock");
        }
        store.put(userId, new CartLine(product.id(), product.name(), product.price(), newQuantity));
        return toResponse(userId);
    }

    public CartResponse removeItem(Long userId, Long productId) {
        if (!store.exists(userId)) {
            throw new NotFoundException("Cart is empty");
        }
        if (!store.remove(userId, productId)) {
            throw new NotFoundException("Product " + productId + " is not in the cart");
        }
        return toResponse(userId);
    }

    public void clear(Long userId) {
        store.clear(userId);
    }

    /** Used by the clean-up batch job: delete carts nobody touched for {@code days} days. */
    public int deleteAbandoned(int days) {
        return store.deleteIdleSince(Instant.now().minus(Duration.ofDays(days)));
    }

    private CartResponse toResponse(Long userId) {
        List<CartItemResponse> items = store.items(userId).stream()
                .map(l -> new CartItemResponse(l.productId(), l.productName(), l.unitPrice(), l.quantity(),
                        l.unitPrice().multiply(BigDecimal.valueOf(l.quantity()))))
                .toList();
        BigDecimal total = items.stream().map(CartItemResponse::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CartResponse(userId, items, total);
    }
}
