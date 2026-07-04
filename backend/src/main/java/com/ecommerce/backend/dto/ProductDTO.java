package com.ecommerce.backend.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record ProductDTO(
                Long id,
                String name,
                String description,
                BigDecimal purchasePriceHT,
                BigDecimal profitMarginPercent,
                BigDecimal vatPercent,
                BigDecimal sellingPriceTTC,
                Integer stockQuantity,
                Boolean active,
                Map<String, String> characteristics,
                List<String> imageUrls) {
}
