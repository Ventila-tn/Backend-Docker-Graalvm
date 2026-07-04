package com.ecommerce.backend.controller;

import com.ecommerce.backend.dto.DashboardStatsDTO;
import com.ecommerce.backend.service.DashboardService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/stats")
    public DashboardStatsDTO getStats(
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start;

        if (from != null && to != null) {
            start = from.atStartOfDay();
            end = to.atTime(23, 59, 59);
        } else {
            start = end.minusDays(days).toLocalDate().atStartOfDay();
        }

        return dashboardService.getStats(start, end);
    }
}
