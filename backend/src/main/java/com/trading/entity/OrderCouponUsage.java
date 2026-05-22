package com.trading.entity;

import com.trading.enums.CouponAudience;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "order_coupon_usages",
        indexes = {
                @Index(name = "idx_order_coupon_usages_order", columnList = "order_id"),
                @Index(name = "idx_order_coupon_usages_buyer_coupon", columnList = "buyer_coupon_id")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OrderCouponUsage {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "coupon_id", nullable = false)
    private Long couponId;

    @Column(name = "buyer_coupon_id", nullable = false)
    private Long buyerCouponId;

    @Column(name = "coupon_name", nullable = false, length = 100)
    private String couponName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CouponAudience audience;

    @Builder.Default
    @Column(nullable = false)
    private Boolean stackable = false;

    @Column(name = "threshold_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal thresholdAmount;

    @Column(name = "coupon_discount_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal couponDiscountAmount;

    @Column(name = "applied_discount_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal appliedDiscountAmount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    private void prePersist() {
        createdAt = LocalDateTime.now();
    }
}
