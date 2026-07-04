package com.ecommerce.backend.dto;

public record CategoryDTO(
        Long id,
        String name,
        Long parentId,
        String parentName) {
}
