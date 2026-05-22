package com.trading.entity;

import com.trading.enums.CouponAudience;
import com.trading.enums.CouponStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "coupons",
        indexes = {
                @Index(name = "idx_coupons_status_valid", columnList = "status,valid_from,valid_to"),
                @Index(name = "idx_coupons_created_at", columnList = "created_at")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Coupon {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(name = "threshold_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal thresholdAmount;

    @Column(name = "discount_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "total_quantity", nullable = false)
    private Integer totalQuantity;

    @Builder.Default
    @Column(name = "claimed_quantity", nullable = false)
    private Integer claimedQuantity = 0;

    @Column(name = "per_user_limit", nullable = false)
    private Integer perUserLimit;

    @Column(name = "valid_from", nullable = false)
    private LocalDateTime validFrom;

    @Column(name = "valid_to", nullable = false)
    private LocalDateTime validTo;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CouponStatus status = CouponStatus.ACTIVE;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CouponAudience audience = CouponAudience.PUBLIC;

    @Builder.Default
    @Column(nullable = false)
    private Boolean stackable = false;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    private void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    private void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
