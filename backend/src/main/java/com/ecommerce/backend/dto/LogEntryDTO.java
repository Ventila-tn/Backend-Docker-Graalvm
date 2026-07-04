package com.ecommerce.backend.dto;

import java.time.LocalDateTime;

public record LogEntryDTO(
        Long id,
        String ipAddress,
        LocalDateTime timestamp,
        String logType,
        String message,
        String details,
        String userAgent,
        String pageUrl
) {
}
