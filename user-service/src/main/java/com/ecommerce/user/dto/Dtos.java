package com.ecommerce.user.dto;

import java.time.Instant;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class Dtos {
    private Dtos() {
    }

    public record RegisterRequest(@NotBlank @Size(min = 3, max = 50) String username,
                                  @NotBlank @Email String email,
                                  @NotBlank @Size(min = 6, max = 100) String password) {
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }

    public record UpdateUserRequest(@Email String email, @Size(min = 6, max = 100) String password) {
    }

    public record TokenResponse(String token, String tokenType, long expiresIn, UserResponse user) {
    }

    public record UserResponse(Long id, String username, String email, String role, Instant createdAt) {
    }
}
