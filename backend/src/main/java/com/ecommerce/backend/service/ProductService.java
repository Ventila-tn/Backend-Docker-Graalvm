package com.ecommerce.backend.service;

import com.ecommerce.backend.entity.Product;
import com.ecommerce.backend.exception.ResourceNotFoundException;
import com.ecommerce.backend.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional
    public Product createOrUpdateProduct(Product product) {
        calculateSellingPrice(product);
        return productRepository.save(product);
    }

    /**
     * Formula: sellingPriceTTC = purchasePriceHT * (1 + (profitMarginPercent /
     * 100)) * (1 + (vatPercent / 100))
     */
    public void calculateSellingPrice(Product product) {
        if (product.getPurchasePriceHT() == null || product.getProfitMarginPercent() == null
                || product.getVatPercent() == null) {
            return;
        }

        BigDecimal marginMultiplier = BigDecimal.ONE.add(
                product.getProfitMarginPercent().divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP));

        BigDecimal vatMultiplier = BigDecimal.ONE.add(
                product.getVatPercent().divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP));

        BigDecimal sellingPrice = product.getPurchasePriceHT()
                .multiply(marginMultiplier)
                .multiply(vatMultiplier)
                .setScale(4, RoundingMode.HALF_UP);

        product.setSellingPriceTTC(sellingPrice);
    }

    @Transactional
    public void deleteOrDeactivate(Long id, StockService stockService) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        product.setActive(false);
        productRepository.save(product);
    }

    @Transactional
    public Product reactivateProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        product.setActive(true);
        return productRepository.save(product);
    }
}
