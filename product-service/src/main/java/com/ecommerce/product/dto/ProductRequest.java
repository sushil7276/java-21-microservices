package com.ecommerce.product.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProductRequest(@NotBlank String name,
                             String description,
                             @NotNull @DecimalMin("0.01") BigDecimal price,
                             @NotNull @Min(0) Integer stock,
                             String category,
                             // optional; only web links are accepted (blocks javascript:, data: and file: URLs)
                             @Size(max = 2000, message = "Image URL is too long (max 2000 characters)")
                             @Pattern(regexp = "^$|^https?://\\S+$", message = "Image URL must start with http:// or https://")
                             String imageUrl) {
}
