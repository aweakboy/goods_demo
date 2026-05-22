package com.trading.entity;

import com.trading.enums.MembershipPurchaseStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "membership_purchases",
        indexes = {
                @Index(name = "idx_membership_purchases_buyer_created", columnList = "buyer_id,created_at"),
                @Index(name = "idx_membership_purchases_out_trade_no", columnList = "out_trade_no")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MembershipPurchase {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "buyer_id", nullable = false)
    private Long buyerId;

    @Column(name = "plan_id", nullable = false)
    private Long planId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MembershipPurchaseStatus status = MembershipPurchaseStatus.PENDING_PAYMENT;

    @Column(name = "out_trade_no", unique = true, length = 64)
    private String outTradeNo;

    @Column(name = "alipay_trade_no", length = 64)
    private String alipayTradeNo;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", insertable = false, updatable = false)
    private MembershipPlan plan;

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
