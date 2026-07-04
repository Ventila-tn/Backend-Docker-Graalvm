package com.ecommerce.backend.repository;

import com.ecommerce.backend.entity.LogEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LogEntryRepository extends JpaRepository<LogEntry, Long> {
    List<LogEntry> findAllByOrderByTimestampDesc();
    List<LogEntry> findByLogTypeOrderByTimestampDesc(String logType);
    List<LogEntry> findByTimestampBetweenOrderByTimestampDesc(LocalDateTime start, LocalDateTime end);
    List<LogEntry> findByIpAddressOrderByTimestampDesc(String ipAddress);
    List<LogEntry> findByLogTypeAndIpAddressOrderByTimestampDesc(String logType, String ipAddress);
    
    @Query("SELECT l FROM LogEntry l WHERE " +
           "(:logType IS NULL OR l.logType = :logType) AND " +
           "(:ipAddress IS NULL OR l.ipAddress = :ipAddress) AND " +
           "(:startDate IS NULL OR l.timestamp >= :startDate) AND " +
           "(:endDate IS NULL OR l.timestamp <= :endDate) " +
           "ORDER BY l.timestamp DESC")
    List<LogEntry> findByFilters(
        @Param("logType") String logType,
        @Param("ipAddress") String ipAddress,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
}
