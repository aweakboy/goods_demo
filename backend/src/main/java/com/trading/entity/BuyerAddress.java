package com.trading.entity;

import com.trading.enums.AddressValidationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "buyer_addresses",
        indexes = {
                @Index(name = "idx_buyer_addresses_buyer_default", columnList = "buyer_id,is_default"),
                @Index(name = "idx_buyer_addresses_buyer_updated", columnList = "buyer_id,updated_at")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BuyerAddress {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "buyer_id", nullable = false)
    private Long buyerId;

    @Column(name = "receiver_name", nullable = false, length = 50)
    private String receiverName;

    @Column(name = "receiver_phone", nullable = false, length = 30)
    private String receiverPhone;

    @Column(nullable = false, length = 50)
    private String province;

    @Column(nullable = false, length = 50)
    private String city;

    @Column(nullable = false, length = 50)
    private String district;

    @Column(name = "detail_address", nullable = false, length = 300)
    private String detailAddress;

    @Column(name = "full_address", nullable = false, length = 500)
    private String fullAddress;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(name = "formatted_address", length = 500)
    private String formattedAddress;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "validation_status", nullable = false, length = 20)
    private AddressValidationStatus validationStatus = AddressValidationStatus.UNVERIFIED;

    @Builder.Default
    @Column(name = "is_default", nullable = false)
    private Boolean defaultAddress = false;

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
