package com.ecommerce.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record StockInboundRequest(
                Long productId,
                String batchNumber,
                Integer quantity,
                BigDecimal unitPrice,
                LocalDate expirationDate,
                String reason) {
}
