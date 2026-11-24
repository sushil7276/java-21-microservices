package com.ecommerce.favourite.dto;

import java.time.Instant;

import com.ecommerce.favourite.client.ProductClient.ProductDto;

public record FavouriteResponse(Long id, Long productId, Instant addedAt, ProductDto product) {
}
