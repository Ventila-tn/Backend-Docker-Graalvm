package com.ecommerce.backend.controller;

import com.ecommerce.backend.dto.ProductStockStats;
import com.ecommerce.backend.dto.StockInboundRequest;
import com.ecommerce.backend.dto.StockMovementResponse;
import com.ecommerce.backend.entity.Product;
import com.ecommerce.backend.exception.ResourceNotFoundException;
import com.ecommerce.backend.repository.ProductRepository;
import com.ecommerce.backend.service.StockService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stock")
@CrossOrigin(origins = "*")
public class StockController {

    private final StockService stockService;
    private final ProductRepository productRepository;

    public StockController(StockService stockService, ProductRepository productRepository) {
        this.stockService = stockService;
        this.productRepository = productRepository;
    }

    @PostMapping("/inbound")
    public void registerInbound(@RequestBody StockInboundRequest request) {
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        stockService.registerInboundMovement(
                product,
                request.batchNumber(),
                request.quantity(),
                request.unitPrice(),
                request.expirationDate(),
                request.reason());
    }

    @GetMapping("/product/{productId}/total")
    public Integer getTotalStock(@PathVariable Long productId) {
        return stockService.getTotalStock(productId);
    }

    @GetMapping("/movements")
    public List<StockMovementResponse> getMovements() {
        return stockService.getAllMovements();
    }

    @GetMapping("/movements/product/{productId}")
    public List<StockMovementResponse> getMovementsByProduct(@PathVariable Long productId) {
        return stockService.getMovementsByProduct(productId);
    }

    @GetMapping("/product/{productId}/stats")
    public ProductStockStats getProductStats(@PathVariable Long productId) {
        return stockService.getProductStats(productId);
    }

    @GetMapping("/product/{productId}/latest-price")
    public java.math.BigDecimal getLatestPrice(@PathVariable Long productId) {
        return stockService.getLatestPurchasePrice(productId);
    }
}
