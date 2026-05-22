package com.trading.entity;

import com.trading.enums.AddressValidationStatus;
import com.trading.enums.ShipmentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "shipments",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_shipments_order", columnNames = "order_id"),
                @UniqueConstraint(name = "uk_shipments_carrier_tracking", columnNames = {"carrier_code", "tracking_number"})
        }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Shipment {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "carrier_code", nullable = false, length = 50)
    private String carrierCode;

    @Column(name = "carrier_name", nullable = false, length = 100)
    private String carrierName;

    @Column(name = "tracking_number", nullable = false, length = 100)
    private String trackingNumber;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ShipmentStatus status = ShipmentStatus.SHIPPED;

    @Column(name = "origin_name", length = 100)
    private String originName;

    @Column(name = "origin_province", length = 50)
    private String originProvince;

    @Column(name = "origin_city", length = 50)
    private String originCity;

    @Column(name = "origin_district", length = 50)
    private String originDistrict;

    @Column(name = "origin_detail_address", length = 300)
    private String originDetailAddress;

    @Column(name = "origin_full_address", length = 500)
    private String originFullAddress;

    @Column(name = "origin_longitude", precision = 10, scale = 7)
    private BigDecimal originLongitude;

    @Column(name = "origin_latitude", precision = 10, scale = 7)
    private BigDecimal originLatitude;

    @Enumerated(EnumType.STRING)
    @Column(name = "origin_address_validation_status", length = 20)
    private AddressValidationStatus originAddressValidationStatus;

    @Column(name = "destination_name", length = 50)
    private String destinationName;

    @Column(name = "destination_phone", length = 30)
    private String destinationPhone;

    @Column(name = "destination_province", length = 50)
    private String destinationProvince;

    @Column(name = "destination_city", length = 50)
    private String destinationCity;

    @Column(name = "destination_district", length = 50)
    private String destinationDistrict;

    @Column(name = "destination_detail_address", length = 300)
    private String destinationDetailAddress;

    @Column(name = "destination_full_address", length = 500)
    private String destinationFullAddress;

    @Column(name = "destination_longitude", precision = 10, scale = 7)
    private BigDecimal destinationLongitude;

    @Column(name = "destination_latitude", precision = 10, scale = 7)
    private BigDecimal destinationLatitude;

    @Enumerated(EnumType.STRING)
    @Column(name = "destination_address_validation_status", length = 20)
    private AddressValidationStatus destinationAddressValidationStatus;

    @Column(name = "shipped_at", nullable = false)
    private LocalDateTime shippedAt;

    @Column(name = "estimated_delivered_at")
    private LocalDateTime estimatedDeliveredAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

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
