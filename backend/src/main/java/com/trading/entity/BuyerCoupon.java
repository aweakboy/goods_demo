package com.trading.entity;

import com.trading.enums.BuyerCouponStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "buyer_coupons",
        indexes = {
                @Index(name = "idx_buyer_coupons_buyer_status", columnList = "buyer_id,status"),
                @Index(name = "idx_buyer_coupons_coupon_buyer", columnList = "coupon_id,buyer_id"),
                @Index(name = "idx_buyer_coupons_used_order", columnList = "used_order_id")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BuyerCoupon {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "buyer_id", nullable = false)
    private Long buyerId;

    @Column(name = "coupon_id", nullable = false)
    private Long couponId;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BuyerCouponStatus status = BuyerCouponStatus.UNUSED;

    @Column(name = "claimed_at", nullable = false, updatable = false)
    private LocalDateTime claimedAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "used_order_id")
    private Long usedOrderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id", insertable = false, updatable = false)
    private Coupon coupon;

    @PrePersist
    private void prePersist() {
        claimedAt = LocalDateTime.now();
    }
}
