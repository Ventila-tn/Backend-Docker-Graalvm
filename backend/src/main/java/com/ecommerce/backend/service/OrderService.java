package com.ecommerce.backend.service;

import com.ecommerce.backend.dto.CheckoutRequest;
import com.ecommerce.backend.entity.Order;
import com.ecommerce.backend.entity.OrderItem;
import com.ecommerce.backend.entity.Product;
import com.ecommerce.backend.enums.OrderStatus;
import com.ecommerce.backend.repository.OrderRepository;
import com.ecommerce.backend.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final StockService stockService;
    private final SettingService settingService;

    public OrderService(OrderRepository orderRepository, ProductRepository productRepository,
                        StockService stockService, SettingService settingService) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.stockService = stockService;
        this.settingService = settingService;
    }

    @Transactional
    public Order placeOrder(CheckoutRequest request) {
        Order order = new Order();
        order.setFirstName(request.firstName());
        order.setLastName(request.lastName());
        order.setAddress(request.address());
        order.setPhone(request.phone());
        order.setEmail(request.email());
        order.setStatus(OrderStatus.PENDING_CONFIRMATION);
        order.setHasStockShortage(false);
        order.setReference(generateReference());

        List<OrderItem> items = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (Map.Entry<Long, Integer> entry : request.items().entrySet()) {
            Product product = productRepository.findById(entry.getKey())
                    .orElseThrow(() -> new RuntimeException("Product not found: " + entry.getKey()));

            Integer stock = stockService.getTotalStock(product.getId());
            Integer requestedQuantity = entry.getValue();

            if (stock < requestedQuantity) {
                order.setHasStockShortage(true);
            }

            if (stock > 0) {
                int quantityToDeduct = Math.min(stock, requestedQuantity);
                stockService.registerOutboundMovement(product, quantityToDeduct, "Order placed");
            }

            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProduct(product);
            item.setQuantity(requestedQuantity);
            item.setUnitPrice(product.getSellingPriceTTC());

            BigDecimal itemTotal = product.getSellingPriceTTC().multiply(BigDecimal.valueOf(requestedQuantity));
            item.setTotalPrice(itemTotal);

            items.add(item);
            totalAmount = totalAmount.add(itemTotal);
        }

        order.setItems(items);
        BigDecimal deliveryFee = settingService.getDeliveryFee();
        order.setDeliveryFee(deliveryFee);
        order.setTotalAmount(totalAmount.add(deliveryFee));

        return orderRepository.save(order);
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAllByOrderByOrderDateDesc();
    }

    private String generateReference() {
        String date = java.time.LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int random = ThreadLocalRandom.current().nextInt(10000, 99999);
        return "CMD-" + date + "-" + random;
    }

    @Transactional
    public Order updateOrderStatus(Long orderId, OrderStatus status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        
        // If canceling and wasn't canceled before, restore stock
        if (status == OrderStatus.CANCELLED && order.getStatus() != OrderStatus.CANCELLED) {
            for (OrderItem item : order.getItems()) {
                Product product = item.getProduct();
                // Restore the quantity that was deducted (or all if none was available before)
                Integer stock = stockService.getTotalStock(product.getId());
                if (stock != null && stock >= 0) {
                    BigDecimal purchasePrice = product.getPurchasePriceHT() != null ? product.getPurchasePriceHT() : BigDecimal.ZERO;
                    stockService.registerInboundMovement(product, "RESTORE-" + orderId, item.getQuantity(), purchasePrice, null, "Order canceled");
                }
            }
        }
        
        order.setStatus(status);
        return orderRepository.save(order);
    }
}
