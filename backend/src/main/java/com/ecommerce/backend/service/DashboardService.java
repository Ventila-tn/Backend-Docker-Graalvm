package com.ecommerce.backend.service;

import com.ecommerce.backend.dto.*;
import com.ecommerce.backend.entity.LogEntry;
import com.ecommerce.backend.entity.Order;
import com.ecommerce.backend.entity.OrderItem;
import com.ecommerce.backend.entity.StockMovement;
import com.ecommerce.backend.enums.MovementType;
import com.ecommerce.backend.enums.OrderStatus;
import com.ecommerce.backend.repository.LogEntryRepository;
import com.ecommerce.backend.repository.OrderRepository;
import com.ecommerce.backend.repository.StockMovementRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final LogEntryRepository logRepo;
    private final OrderRepository orderRepo;
    private final StockMovementRepository stockMovementRepo;

    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public DashboardService(LogEntryRepository logRepo, OrderRepository orderRepo,
                            StockMovementRepository stockMovementRepo) {
        this.logRepo = logRepo;
        this.orderRepo = orderRepo;
        this.stockMovementRepo = stockMovementRepo;
    }

    public DashboardStatsDTO getStats(LocalDateTime from, LocalDateTime to) {

        // ── Visits (counted by page view, not by IP) ──────────────────────────
        List<LogEntry> logs = logRepo.findByTimestampBetweenOrderByTimestampDesc(from, to);

        // A "visit" = any log entry that has a pageUrl (page navigation event)
        List<LogEntry> pageViews = logs.stream()
                .filter(l -> l.getPageUrl() != null && !l.getPageUrl().isBlank())
                .collect(Collectors.toList());

        long totalVisits    = pageViews.size();
        long uniqueVisitors = pageViews.stream().map(LogEntry::getIpAddress).distinct().count();

        // Visits by day — unique visitors per day (by IP)
        List<DayStatDTO> visitsByDay = pageViews.stream()
                .collect(Collectors.groupingBy(
                        l -> l.getTimestamp().format(DAY_FMT),
                        Collectors.collectingAndThen(
                                Collectors.mapping(LogEntry::getIpAddress, Collectors.toSet()),
                                set -> (long) set.size()
                        )))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> new DayStatDTO(e.getKey(), e.getValue()))
                .collect(Collectors.toList());

        // Visits by hour — unique visitors per hour (by IP)
        List<HourStatDTO> visitsByHour = new ArrayList<>();
        for (int h = 0; h < 24; h++) {
            final int hour = h;
            long uniqueCount = pageViews.stream()
                    .filter(l -> l.getTimestamp().getHour() == hour)
                    .map(LogEntry::getIpAddress)
                    .distinct()
                    .count();
            visitsByHour.add(new HourStatDTO(h, uniqueCount));
        }

        // Top IPs
        List<IpStatDTO> topIps = pageViews.stream()
                .collect(Collectors.groupingBy(LogEntry::getIpAddress))
                .entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue().size(), a.getValue().size()))
                .limit(20)
                .map(e -> {
                    var ipLogs = e.getValue();
                    String first = ipLogs.stream().map(LogEntry::getTimestamp).min(Comparator.naturalOrder())
                            .map(t -> t.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))).orElse("-");
                    String last  = ipLogs.stream().map(LogEntry::getTimestamp).max(Comparator.naturalOrder())
                            .map(t -> t.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))).orElse("-");
                    return new IpStatDTO(e.getKey(), ipLogs.size(), first, last);
                })
                .collect(Collectors.toList());

        // Top pages
        List<PageStatDTO> topPages = pageViews.stream()
                .collect(Collectors.groupingBy(LogEntry::getPageUrl, Collectors.counting()))
                .entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(5)
                .map(e -> new PageStatDTO(e.getKey(), e.getValue()))
                .collect(Collectors.toList());

        // ── Orders ─────────────────────────────────────────────────────────────
        List<Order> allOrders = orderRepo.findAllByOrderByOrderDateDesc().stream()
                .filter(o -> !o.getOrderDate().isBefore(from) && !o.getOrderDate().isAfter(to))
                .collect(Collectors.toList());

        long totalOrders = allOrders.size();

        Map<String, Long> ordersByStatus = allOrders.stream()
                .collect(Collectors.groupingBy(o -> o.getStatus().name(), Collectors.counting()));

        List<DayStatDTO> ordersByDay = allOrders.stream()
                .collect(Collectors.groupingBy(o -> o.getOrderDate().format(DAY_FMT), Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> new DayStatDTO(e.getKey(), e.getValue()))
                .collect(Collectors.toList());

        // ── Revenue & Profit (PAID only) ───────────────────────────────────────
        List<Order> paidOrders = allOrders.stream()
                .filter(o -> o.getStatus() == OrderStatus.PAID)
                .collect(Collectors.toList());

        BigDecimal totalRevenue = paidOrders.stream()
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalProfit = paidOrders.stream()
                .flatMap(o -> o.getItems().stream())
                .map(this::calculateItemProfit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // ── Stock cost (inbound movements in period) ───────────────────────────
        List<StockMovement> inbounds = stockMovementRepo
                .findByTypeAndMovementDateBetween(MovementType.IN, from, to);

        BigDecimal stockCost = inbounds.stream()
                .map(m -> m.getUnitPrice().multiply(BigDecimal.valueOf(m.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalStockInbound = inbounds.stream()
                .mapToLong(StockMovement::getQuantity)
                .sum();

        // Total current stock units across all products (sum of all batch currentQuantity)
        long totalStockUnits = stockMovementRepo.findAll().stream()
                .filter(m -> m.getType() == MovementType.IN)
                .mapToLong(StockMovement::getQuantity).sum()
                - stockMovementRepo.findAll().stream()
                .filter(m -> m.getType() == MovementType.OUT)
                .mapToLong(StockMovement::getQuantity).sum();

        // Net profit = gross profit from sales - stock purchasing cost
        BigDecimal netProfit = totalProfit.subtract(stockCost);

        return new DashboardStatsDTO(
                totalVisits, uniqueVisitors, visitsByDay, visitsByHour, topIps, topPages,
                totalOrders, ordersByStatus, ordersByDay,
                totalRevenue.setScale(2, RoundingMode.HALF_UP),
                totalProfit.setScale(2, RoundingMode.HALF_UP),
                stockCost.setScale(2, RoundingMode.HALF_UP),
                netProfit.setScale(2, RoundingMode.HALF_UP),
                totalStockUnits,
                totalStockInbound
        );
    }

    private BigDecimal calculateItemProfit(OrderItem item) {
        BigDecimal vatDivisor = BigDecimal.ONE.add(
                item.getProduct().getVatPercent().divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP));
        BigDecimal sellingHT = item.getUnitPrice().divide(vatDivisor, 4, RoundingMode.HALF_UP);
        BigDecimal profitPerUnit = sellingHT.subtract(item.getProduct().getPurchasePriceHT());
        return profitPerUnit.multiply(BigDecimal.valueOf(item.getQuantity()));
    }
}
