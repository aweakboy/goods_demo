package com.trading.repository;

import com.trading.entity.OrderCouponUsage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderCouponUsageRepository extends JpaRepository<OrderCouponUsage, Long> {
    List<OrderCouponUsage> findByOrderIdOrderByIdAsc(Long orderId);

    List<OrderCouponUsage> findByOrderIdInOrderByOrderIdAscIdAsc(List<Long> orderIds);
}
