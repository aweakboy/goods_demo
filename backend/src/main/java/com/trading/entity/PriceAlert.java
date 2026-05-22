package com.trading.entity;

import com.trading.enums.PriceAlertStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "price_alerts",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_price_alerts_buyer_product", columnNames = {"buyer_id", "product_id"})
        },
        indexes = {
                @Index(name = "idx_price_alerts_buyer_updated", columnList = "buyer_id,updated_at"),
                @Index(name = "idx_price_alerts_product_status_target", columnList = "product_id,status,target_price")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PriceAlert {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "buyer_id", nullable = false)
    private Long buyerId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "target_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal targetPrice;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PriceAlertStatus status = PriceAlertStatus.ACTIVE;

    @Column(name = "last_notified_price", precision = 10, scale = 2)
    private BigDecimal lastNotifiedPrice;

    @Column(name = "triggered_at")
    private LocalDateTime triggeredAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", insertable = false, updatable = false)
    private Product product;

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
