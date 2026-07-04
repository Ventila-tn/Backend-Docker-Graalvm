package com.ecommerce.backend.dto;

public record CategoryRequest(
        String name,
        Long parentId) {
}
