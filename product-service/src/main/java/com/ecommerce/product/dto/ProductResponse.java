package com.ecommerce.product.dto;

import java.math.BigDecimal;
import java.time.Instant;

/** What the API returns for a product (we never return the database entity directly). */
public record ProductResponse(Long id, String name, String description, BigDecimal price,
                              Integer stock, String category, String imageUrl, Instant createdAt) {
}
