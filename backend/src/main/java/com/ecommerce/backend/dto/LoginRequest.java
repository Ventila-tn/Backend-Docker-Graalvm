package com.ecommerce.backend.dto;

public record LoginRequest(
        String username,
        String password) {
}
