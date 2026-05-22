package com.trading.dto;

import com.trading.entity.OrderCouponUsage;
import com.trading.enums.CouponAudience;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class OrderCouponUsageResponse {
    private Long id;
    private Long orderId;
    private Long couponId;
    private Long buyerCouponId;
    private String couponName;
    private CouponAudience audience;
    private Boolean stackable;
    private BigDecimal thresholdAmount;
    private BigDecimal couponDiscountAmount;
    private BigDecimal appliedDiscountAmount;
    private LocalDateTime createdAt;

    public static OrderCouponUsageResponse from(OrderCouponUsage usage) {
        return OrderCouponUsageResponse.builder()
                .id(usage.getId())
                .orderId(usage.getOrderId())
                .couponId(usage.getCouponId())
                .buyerCouponId(usage.getBuyerCouponId())
                .couponName(usage.getCouponName())
                .audience(usage.getAudience())
                .stackable(Boolean.TRUE.equals(usage.getStackable()))
                .thresholdAmount(usage.getThresholdAmount())
                .couponDiscountAmount(usage.getCouponDiscountAmount())
                .appliedDiscountAmount(usage.getAppliedDiscountAmount())
                .createdAt(usage.getCreatedAt())
                .build();
    }
}
