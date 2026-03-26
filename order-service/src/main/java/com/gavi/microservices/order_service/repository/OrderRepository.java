package com.gavi.microservices.order_service.repository;

import com.gavi.microservices.order_service.model.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    
    Order findByOrderNumber(String orderNumber);
    
    Page<Order> findBySkuCode(String skuCode, Pageable pageable);
    
    @Query("SELECT o FROM Order o WHERE " +
           "(:minPrice IS NULL OR o.price >= :minPrice) AND " +
           "(:maxPrice IS NULL OR o.price <= :maxPrice) AND " +
           "(:minQuantity IS NULL OR o.quantity >= :minQuantity) AND " +
           "(:maxQuantity IS NULL OR o.quantity <= :maxQuantity) AND " +
           "(:startDate IS NULL OR o.createdAt >= :startDate) AND " +
           "(:endDate IS NULL OR o.createdAt <= :endDate)")
    Page<Order> findOrdersWithFilters(
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("minQuantity") Integer minQuantity,
            @Param("maxQuantity") Integer maxQuantity,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);
}
