package com.ecommerce.backend.repository;

import com.ecommerce.backend.entity.StockMovement;
import com.ecommerce.backend.enums.MovementType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {
    List<StockMovement> findAllByOrderByMovementDateDesc();
    List<StockMovement> findAllByProduct_IdOrderByMovementDateDesc(Long productId);
    List<StockMovement> findByTypeAndMovementDateBetween(MovementType type, LocalDateTime from, LocalDateTime to);
}
