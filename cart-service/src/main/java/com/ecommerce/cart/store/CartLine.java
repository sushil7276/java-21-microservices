package com.ecommerce.cart.store;

import java.math.BigDecimal;

/** One product in a cart. The price is a copy taken when the product was added (product-service stays the source of truth). */
public record CartLine(Long productId, String productName, BigDecimal unitPrice, Integer quantity) {
}
