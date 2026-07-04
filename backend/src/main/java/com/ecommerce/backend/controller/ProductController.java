package com.ecommerce.backend.controller;

import com.ecommerce.backend.dto.ProductDTO;
import com.ecommerce.backend.dto.ProductRequest;
import com.ecommerce.backend.entity.Product;
import com.ecommerce.backend.exception.ResourceNotFoundException;
import com.ecommerce.backend.repository.ProductRepository;
import com.ecommerce.backend.service.ProductService;
import com.ecommerce.backend.service.StockService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "*")
public class ProductController {

    private final ProductService productService;
    private final ProductRepository productRepository;
    private final StockService stockService;

    public ProductController(ProductService productService, ProductRepository productRepository,
            StockService stockService) {
        this.productService = productService;
        this.productRepository = productRepository;
        this.stockService = stockService;
    }

    @GetMapping
    public List<ProductDTO> getAllProducts() {
        return productRepository.findAllByActiveTrue().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @GetMapping("/archived")
    public List<ProductDTO> getArchivedProducts() {
        return productRepository.findAllByActiveFalse().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ProductDTO getProduct(@PathVariable Long id) {
        return productRepository.findById(id)
                .map(this::mapToDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
    }

    @PostMapping
    public ProductDTO createProduct(@RequestBody ProductRequest request) {
        Product product = new Product();
        mapToEntity(request, product);
        Product saved = productService.createOrUpdateProduct(product);
        return mapToDTO(saved);
    }

    @PutMapping("/{id}")
    public ProductDTO updateProduct(@PathVariable Long id, @RequestBody ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        mapToEntity(request, product);
        Product saved = productService.createOrUpdateProduct(product);
        return mapToDTO(saved);
    }

    @DeleteMapping("/{id}")
    public void deleteProduct(@PathVariable Long id) {
        productService.deleteOrDeactivate(id, stockService);
    }

    @PostMapping("/{id}/reactivate")
    public ProductDTO reactivateProduct(@PathVariable Long id) {
        Product reactivated = productService.reactivateProduct(id);
        return mapToDTO(reactivated);
    }

    private ProductDTO mapToDTO(Product p) {
        return new ProductDTO(
                p.getId(),
                p.getName(),
                p.getDescription(),
                p.getPurchasePriceHT(),
                p.getProfitMarginPercent(),
                p.getVatPercent(),
                p.getSellingPriceTTC(),
                stockService.getTotalStock(p.getId()),
                p.isActive(),
                p.getCharacteristics(),
                p.getImageUrls() != null ? p.getImageUrls() : List.of());
    }

    private void mapToEntity(ProductRequest request, Product product) {
        product.setName(request.name());
        product.setDescription(request.description());
        product.setPurchasePriceHT(request.purchasePriceHT());
        product.setProfitMarginPercent(request.profitMarginPercent());
        product.setVatPercent(request.vatPercent());
        product.setCharacteristics(request.characteristics());
        product.setImageUrls(request.imageUrls() != null ? request.imageUrls() : List.of());
    }
}
