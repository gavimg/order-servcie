package com.gavi.microservices.order_service.service;

import com.gavi.microservices.order_service.client.InventoryServiceClient;
import com.gavi.microservices.order_service.dto.OrderRequest;
import com.gavi.microservices.order_service.dto.OrderResponse;
import com.gavi.microservices.order_service.exception.OrderNotFoundException;
import com.gavi.microservices.order_service.model.Order;
import com.gavi.microservices.order_service.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final InventoryServiceClient inventoryServiceClient;

    public void placeOrder(OrderRequest orderRequest) {
        var productInStock = inventoryServiceClient.isInStock(orderRequest.skuCode(), orderRequest.quantity());
        if (!productInStock) {
            log.info("Product with SkuCode {} is not in stock", orderRequest.skuCode());
            throw new RuntimeException(String.format("Product with SkuCode %s is not in stock", orderRequest.skuCode()));
        }

        Order order = new Order();
        order.setOrderNumber(UUID.randomUUID().toString());
        order.setSkuCode(orderRequest.skuCode());
        order.setPrice(orderRequest.price());
        order.setQuantity(orderRequest.quantity());
        orderRepository.save(order);
        log.info("Order saved successfully with order number: {}", order.getOrderNumber());
    }

    public Page<OrderResponse> getOrders(BigDecimal minPrice, BigDecimal maxPrice, 
                                        Integer minQuantity, Integer maxQuantity,
                                        LocalDateTime startDate, LocalDateTime endDate,
                                        Pageable pageable) {
        log.info("Fetching orders with filters - minPrice: {}, maxPrice: {}, minQuantity: {}, maxQuantity: {}, startDate: {}, endDate: {}", 
                minPrice, maxPrice, minQuantity, maxQuantity, startDate, endDate);
        
        Page<Order> orders = orderRepository.findOrdersWithFilters(
                minPrice, maxPrice, minQuantity, maxQuantity, startDate, endDate, pageable);
        
        return orders.map(this::convertToOrderResponse);
    }

    public OrderResponse getOrderByOrderNumber(String orderNumber) {
        log.info("Fetching order by order number: {}", orderNumber);
        
        Order order = orderRepository.findByOrderNumber(orderNumber);
        if (order == null) {
            log.error("Order not found with order number: {}", orderNumber);
            throw new OrderNotFoundException("Order not found with order number: " + orderNumber);
        }
        
        return convertToOrderResponse(order);
    }

    public Page<OrderResponse> getOrdersBySkuCode(String skuCode, Pageable pageable) {
        log.info("Fetching orders by SKU code: {}", skuCode);
        
        Page<Order> orders = orderRepository.findBySkuCode(skuCode, pageable);
        return orders.map(this::convertToOrderResponse);
    }

    private OrderResponse convertToOrderResponse(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getSkuCode(),
                order.getPrice(),
                order.getQuantity()
        );
    }
}
