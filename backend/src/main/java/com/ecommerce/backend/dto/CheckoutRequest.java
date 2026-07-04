package com.ecommerce.backend.dto;

import java.util.Map;

public record CheckoutRequest(
    String firstName,
    String lastName,
    String address,
    String phone,
    String email,
    Map<Long, Integer> items
) {
}
