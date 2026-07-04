package com.ecommerce.backend.dto;

public record RegisterRequest(
        String username,
        String password,
        String role // ROLE_ADMIN or ROLE_CLIENT
) {
}
