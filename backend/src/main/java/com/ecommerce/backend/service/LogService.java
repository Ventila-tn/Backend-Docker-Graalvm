package com.ecommerce.backend.service;

import com.ecommerce.backend.dto.LogEntryDTO;
import com.ecommerce.backend.dto.LogEntryRequest;
import com.ecommerce.backend.entity.LogEntry;
import com.ecommerce.backend.repository.LogEntryRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LogService {

    private final LogEntryRepository logEntryRepository;

    public LogService(LogEntryRepository logEntryRepository) {
        this.logEntryRepository = logEntryRepository;
    }

    public LogEntryDTO createLog(LogEntryRequest request, HttpServletRequest httpRequest) {
        LogEntry logEntry = new LogEntry();
        logEntry.setIpAddress(getClientIpAddress(httpRequest));
        logEntry.setTimestamp(LocalDateTime.now());
        logEntry.setLogType(request.logType());
        logEntry.setMessage(request.message());
        logEntry.setDetails(request.details());
        logEntry.setUserAgent(request.userAgent());
        logEntry.setPageUrl(request.pageUrl());

        LogEntry saved = logEntryRepository.save(logEntry);
        return mapToDTO(saved);
    }

    public List<LogEntryDTO> getAllLogs() {
        return logEntryRepository.findAllByOrderByTimestampDesc().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public List<LogEntryDTO> getLogsByType(String logType) {
        return logEntryRepository.findByLogTypeOrderByTimestampDesc(logType).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }
    
    public List<LogEntryDTO> getLogsByFilters(String logType, String ipAddress, LocalDateTime startDate, LocalDateTime endDate) {
        return logEntryRepository.findByFilters(logType, ipAddress, startDate, endDate).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }
    
    public List<String> getAllDistinctIpAddresses() {
        return logEntryRepository.findAll().stream()
                .map(LogEntry::getIpAddress)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }
        // If multiple IPs, take the first one
        if (ipAddress != null && ipAddress.contains(",")) {
            ipAddress = ipAddress.split(",")[0].trim();
        }
        return ipAddress;
    }

    private LogEntryDTO mapToDTO(LogEntry logEntry) {
        return new LogEntryDTO(
                logEntry.getId(),
                logEntry.getIpAddress(),
                logEntry.getTimestamp(),
                logEntry.getLogType(),
                logEntry.getMessage(),
                logEntry.getDetails(),
                logEntry.getUserAgent(),
                logEntry.getPageUrl()
        );
    }
}
