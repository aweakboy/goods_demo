package com.trading.entity;

import com.trading.enums.BuyerNotificationType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "buyer_notifications",
        indexes = {
                @Index(name = "idx_buyer_notifications_buyer_created", columnList = "buyer_id,created_at"),
                @Index(name = "idx_buyer_notifications_buyer_read", columnList = "buyer_id,read_at")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BuyerNotification {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "buyer_id", nullable = false)
    private Long buyerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BuyerNotificationType type;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false, length = 500)
    private String content;

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "shop_id")
    private Long shopId;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", insertable = false, updatable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", insertable = false, updatable = false)
    private Shop shop;

    @PrePersist
    private void prePersist() {
        createdAt = LocalDateTime.now();
    }
}
