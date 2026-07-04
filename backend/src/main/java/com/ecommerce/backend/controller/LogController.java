package com.ecommerce.backend.controller;

import com.ecommerce.backend.dto.LogEntryDTO;
import com.ecommerce.backend.dto.LogEntryRequest;
import com.ecommerce.backend.service.LogService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/logs")
@CrossOrigin(origins = "*")
public class LogController {

    private final LogService logService;

    public LogController(LogService logService) {
        this.logService = logService;
    }

    @PostMapping
    public LogEntryDTO createLog(@RequestBody LogEntryRequest request, HttpServletRequest httpRequest) {
        return logService.createLog(request, httpRequest);
    }

    @GetMapping
    public List<LogEntryDTO> getAllLogs(
            @RequestParam(required = false) String logType,
            @RequestParam(required = false) String ipAddress,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate
    ) {
        if (logType == null && ipAddress == null && startDate == null && endDate == null) {
            return logService.getAllLogs();
        }
        return logService.getLogsByFilters(logType, ipAddress, startDate, endDate);
    }

    @GetMapping("/type/{logType}")
    public List<LogEntryDTO> getLogsByType(@PathVariable String logType) {
        return logService.getLogsByType(logType);
    }
    
    @GetMapping("/ips")
    public List<String> getAllIpAddresses() {
        return logService.getAllDistinctIpAddresses();
    }
}
