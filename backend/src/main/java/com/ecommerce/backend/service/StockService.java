package com.ecommerce.backend.service;

import com.ecommerce.backend.dto.ProductStockStats;
import com.ecommerce.backend.dto.StockMovementResponse;
import com.ecommerce.backend.entity.Product;
import com.ecommerce.backend.entity.StockBatch;
import com.ecommerce.backend.entity.StockMovement;
import com.ecommerce.backend.enums.MovementType;
import com.ecommerce.backend.repository.ProductRepository;
import com.ecommerce.backend.repository.StockBatchRepository;
import com.ecommerce.backend.repository.StockMovementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class StockService {

        private final StockBatchRepository batchRepository;
        private final StockMovementRepository movementRepository;
        private final ProductRepository productRepository;

        public StockService(StockBatchRepository batchRepository, StockMovementRepository movementRepository,
                        ProductRepository productRepository) {
                this.batchRepository = batchRepository;
                this.movementRepository = movementRepository;
                this.productRepository = productRepository;
        }

        @Transactional
        public void registerInboundMovement(Product product, String batchNumber, Integer quantity, BigDecimal unitPrice,
                        LocalDate expirationDate, String reason) {
                // 1. Find or create the batch
                Optional<StockBatch> existingBatch = batchRepository.findByProductIdAndBatchNumber(product.getId(),
                                batchNumber);

                StockBatch batch;
                if (existingBatch.isPresent()) {
                        batch = existingBatch.get();
                        batch.setCurrentQuantity(batch.getCurrentQuantity() + quantity);
                        // On garde le prix d'achat initial du lot ou on le met à jour ?
                        // Ici on met à jour pour refléter le dernier prix
                        batch.setUnitPrice(unitPrice);
                } else {
                        batch = new StockBatch();
                        batch.setProduct(product);
                        batch.setBatchNumber(batchNumber);
                        batch.setInitialQuantity(quantity);
                        batch.setCurrentQuantity(quantity);
                        batch.setUnitPrice(unitPrice);
                        batch.setExpirationDate(expirationDate);
                }
                batchRepository.save(batch);

                // 2. Record movement history
                StockMovement movement = new StockMovement();
                movement.setProduct(product);
                movement.setStockBatch(batch);
                movement.setType(MovementType.IN);
                movement.setQuantity(quantity);
                movement.setUnitPrice(unitPrice);
                movement.setReason(reason);
                movementRepository.save(movement);

                // 3. Update Product base purchase price and recalculate selling price
                product.setPurchasePriceHT(unitPrice);

                // Recalcul du prix de vente TTC : PrixHT * (1 + Marge/100) * (1 + TVA/100)
                BigDecimal marginMultiplier = BigDecimal.ONE
                                .add(product.getProfitMarginPercent().divide(new BigDecimal("100"), 4,
                                                RoundingMode.HALF_UP));
                BigDecimal taxMultiplier = BigDecimal.ONE
                                .add(product.getVatPercent().divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP));
                BigDecimal newSellingPrice = unitPrice.multiply(marginMultiplier).multiply(taxMultiplier).setScale(2,
                                RoundingMode.HALF_UP);

                product.setSellingPriceTTC(newSellingPrice);
                productRepository.save(product);
        }

        @Transactional
        public boolean registerOutboundMovement(Product product, Integer quantity, String reason) {
                int remainingToDeduct = quantity;
                List<StockBatch> batches = batchRepository.findByProductIdOrderByExpirationDateAsc(product.getId());

                for (StockBatch batch : batches) {
                        if (remainingToDeduct <= 0) break;

                        int available = batch.getCurrentQuantity();
                        if (available > 0) {
                                int toDeduct = Math.min(available, remainingToDeduct);
                                batch.setCurrentQuantity(available - toDeduct);
                                batchRepository.save(batch);

                                StockMovement movement = new StockMovement();
                                movement.setProduct(product);
                                movement.setStockBatch(batch);
                                movement.setType(MovementType.OUT);
                                movement.setQuantity(toDeduct);
                                movement.setUnitPrice(batch.getUnitPrice());
                                movement.setReason(reason);
                                movementRepository.save(movement);

                                remainingToDeduct -= toDeduct;
                        }
                }

                return remainingToDeduct <= 0;
        }

        public Integer getTotalStock(Long productId) {
                return batchRepository.findByProductId(productId).stream()
                                .mapToInt(StockBatch::getCurrentQuantity)
                                .sum();
        }

        @Transactional(readOnly = true)
        public List<StockMovementResponse> getAllMovements() {
                return movementRepository.findAllByOrderByMovementDateDesc().stream()
                                .map(this::mapToResponse)
                                .collect(Collectors.toList());
        }

        @Transactional(readOnly = true)
        public List<StockMovementResponse> getMovementsByProduct(Long productId) {
                return movementRepository.findAllByProduct_IdOrderByMovementDateDesc(productId).stream()
                                .map(this::mapToResponse)
                                .collect(Collectors.toList());
        }

        @Transactional(readOnly = true)
        public ProductStockStats getProductStats(Long productId) {
                List<StockMovement> movements = movementRepository
                                .findAllByProduct_IdOrderByMovementDateDesc(productId);

                int totalUnits = getTotalStock(productId);

                List<StockMovement> inMovements = movements.stream()
                                .filter(m -> m.getType() == MovementType.IN)
                                .toList();

                long totalInQuantity = inMovements.stream()
                                .mapToLong(StockMovement::getQuantity)
                                .sum();

                BigDecimal totalPurchaseValue = inMovements.stream()
                                .map(m -> m.getUnitPrice().multiply(new BigDecimal(m.getQuantity())))
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal averagePurchasePrice = totalInQuantity > 0
                                ? totalPurchaseValue.divide(new BigDecimal(totalInQuantity), 2, RoundingMode.HALF_UP)
                                : BigDecimal.ZERO;

                return new ProductStockStats(totalUnits, averagePurchasePrice, totalPurchaseValue);
        }

        @Transactional(readOnly = true)
        public BigDecimal getLatestPurchasePrice(Long productId) {
                return movementRepository.findAllByProduct_IdOrderByMovementDateDesc(productId).stream()
                                .filter(m -> m.getType() == MovementType.IN)
                                .map(StockMovement::getUnitPrice)
                                .findFirst()
                                .orElse(BigDecimal.ZERO);
        }

        private StockMovementResponse mapToResponse(StockMovement m) {
                return new StockMovementResponse(
                                m.getId(),
                                m.getMovementDate(),
                                m.getType(),
                                m.getQuantity(),
                                m.getUnitPrice(),
                                m.getReason(),
                                m.getProduct().getId(),
                                m.getProduct().getName(),
                                m.getStockBatch() != null ? m.getStockBatch().getBatchNumber() : null);
        }
}
