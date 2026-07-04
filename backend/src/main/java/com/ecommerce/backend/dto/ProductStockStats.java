package com.ecommerce.backend.dto;

import java.math.BigDecimal;

public record ProductStockStats(
        Integer totalUnits,
        BigDecimal averagePurchasePrice,
        BigDecimal totalPurchaseValue) {
}
