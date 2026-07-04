package com.ecommerce.backend.repository;

import com.ecommerce.backend.entity.StockBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface StockBatchRepository extends JpaRepository<StockBatch, Long> {
    List<StockBatch> findByProductId(Long productId);

    Optional<StockBatch> findByProductIdAndBatchNumber(Long productId, String batchNumber);

    @Query("SELECT b FROM StockBatch b WHERE b.product.id = :productId AND b.currentQuantity > 0 ORDER BY CASE WHEN b.expirationDate IS NULL THEN 1 ELSE 0 END, b.expirationDate ASC")
    List<StockBatch> findByProductIdOrderByExpirationDateAsc(@Param("productId") Long productId);
}
