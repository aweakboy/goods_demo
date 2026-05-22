package com.trading.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "membership_monthly_benefits",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_membership_monthly_benefit", columnNames = {"buyer_id", "plan_id", "benefit_month"})
        },
        indexes = {
                @Index(name = "idx_membership_monthly_benefits_buyer", columnList = "buyer_id,benefit_month")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MembershipMonthlyBenefit {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "buyer_id", nullable = false)
    private Long buyerId;

    @Column(name = "plan_id", nullable = false)
    private Long planId;

    @Column(name = "benefit_month", nullable = false, length = 7)
    private String benefitMonth;

    @Column(name = "coupon_id", nullable = false)
    private Long couponId;

    @Column(name = "buyer_coupon_id", nullable = false)
    private Long buyerCouponId;

    @Column(name = "claimed_at", nullable = false)
    private LocalDateTime claimedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", insertable = false, updatable = false)
    private MembershipPlan plan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id", insertable = false, updatable = false)
    private Coupon coupon;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_coupon_id", insertable = false, updatable = false)
    private BuyerCoupon buyerCoupon;
}
