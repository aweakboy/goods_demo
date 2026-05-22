package com.trading.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "shop_favorites",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_shop_favorites_buyer_shop", columnNames = {"buyer_id", "shop_id"})
        },
        indexes = {
                @Index(name = "idx_shop_favorites_buyer_created", columnList = "buyer_id,created_at"),
                @Index(name = "idx_shop_favorites_shop", columnList = "shop_id")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ShopFavorite {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "buyer_id", nullable = false)
    private Long buyerId;

    @Column(name = "shop_id", nullable = false)
    private Long shopId;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", insertable = false, updatable = false)
    private Shop shop;

    @PrePersist
    private void prePersist() {
        createdAt = LocalDateTime.now();
    }
}
