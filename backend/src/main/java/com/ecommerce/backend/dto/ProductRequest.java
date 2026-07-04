package com.ecommerce.backend.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record ProductRequest(
                String name,
                String description,
                BigDecimal purchasePriceHT,
                BigDecimal profitMarginPercent,
                BigDecimal vatPercent,
                Map<String, String> characteristics,
                List<String> imageUrls) {
}
