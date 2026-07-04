package com.ecommerce.backend.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record DashboardStatsDTO(
    long totalVisits,
    long uniqueVisitors,
    List<DayStatDTO> visitsByDay,
    List<HourStatDTO> visitsByHour,
    List<IpStatDTO> topIps,
    List<PageStatDTO> topPages,
    long totalOrders,
    Map<String, Long> ordersByStatus,
    List<DayStatDTO> ordersByDay,
    BigDecimal totalRevenue,
    BigDecimal totalProfit,
    BigDecimal stockCost,
    BigDecimal netProfit,
    long totalStockUnits,
    long totalStockInbound
) {}
