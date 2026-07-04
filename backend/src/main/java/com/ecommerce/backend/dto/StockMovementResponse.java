package com.ecommerce.backend.dto;

import com.ecommerce.backend.enums.MovementType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record StockMovementResponse(
        Long id,
        LocalDateTime movementDate,
        MovementType type,
        Integer quantity,
        BigDecimal unitPrice,
        String reason,
        Long productId,
        String productName,
        String batchNumber) {
}
