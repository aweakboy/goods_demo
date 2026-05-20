package com.trading.repository;

import com.trading.entity.Order;
import com.trading.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByBuyerIdOrderByCreatedAtDesc(Long buyerId);

    List<Order> findByBuyerIdAndStatusOrderByCreatedAtDesc(Long buyerId, OrderStatus status);

    @Query("SELECT DISTINCT o FROM Order o JOIN o.items i WHERE i.productId IN " +
           "(SELECT p.id FROM Product p WHERE p.sellerId = :sellerId) " +
           "ORDER BY o.createdAt DESC")
    List<Order> findSellerOrders(@Param("sellerId") Long sellerId);

    @Query("SELECT DISTINCT o FROM Order o JOIN o.items i WHERE i.productId IN " +
           "(SELECT p.id FROM Product p WHERE p.sellerId = :sellerId) " +
           "AND o.status = :status ORDER BY o.createdAt DESC")
    List<Order> findSellerOrdersByStatus(@Param("sellerId") Long sellerId,
                                         @Param("status") OrderStatus status);

    @Query("SELECT o FROM Order o WHERE :status IS NULL OR o.status = :status ORDER BY o.createdAt DESC")
    Page<Order> findAllByAdminFilter(@Param("status") OrderStatus status, Pageable pageable);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status = 'COMPLETED'")
    BigDecimal sumCompletedAmount();

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
}
