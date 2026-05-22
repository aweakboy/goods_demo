package com.trading.entity;

import com.trading.enums.BuyerMembershipStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "buyer_memberships",
        indexes = {
                @Index(name = "idx_buyer_memberships_buyer", columnList = "buyer_id"),
                @Index(name = "idx_buyer_memberships_status_expires", columnList = "status,expires_at")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BuyerMembership {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "buyer_id", nullable = false, unique = true)
    private Long buyerId;

    @Column(name = "plan_id", nullable = false)
    private Long planId;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BuyerMembershipStatus status = BuyerMembershipStatus.ACTIVE;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "last_paid_at")
    private LocalDateTime lastPaidAt;

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
