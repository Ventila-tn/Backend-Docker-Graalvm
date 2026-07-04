package com.ecommerce.backend.dto;

public record LogEntryRequest(
        String logType,
        String message,
        String details,
        String userAgent,
        String pageUrl
) {
}
