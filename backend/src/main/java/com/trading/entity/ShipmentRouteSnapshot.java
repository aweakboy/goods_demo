package com.trading.entity;

import com.trading.enums.RoutePlanningStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "shipment_route_snapshots",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_shipment_route_snapshot_shipment", columnNames = "shipment_id")
        }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ShipmentRouteSnapshot {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "shipment_id", nullable = false)
    private Long shipmentId;

    @Column(nullable = false, length = 30)
    private String provider;

    @Enumerated(EnumType.STRING)
    @Column(name = "planning_status", nullable = false, length = 20)
    private RoutePlanningStatus planningStatus;

    @Column(name = "origin_longitude", precision = 10, scale = 7)
    private BigDecimal originLongitude;

    @Column(name = "origin_latitude", precision = 10, scale = 7)
    private BigDecimal originLatitude;

    @Column(name = "destination_longitude", precision = 10, scale = 7)
    private BigDecimal destinationLongitude;

    @Column(name = "destination_latitude", precision = 10, scale = 7)
    private BigDecimal destinationLatitude;

    @Lob
    @Column(name = "route_polyline_json", columnDefinition = "LONGTEXT")
    private String routePolylineJson;

    @Column(name = "distance_meters")
    private Long distanceMeters;

    @Column(name = "duration_seconds")
    private Long durationSeconds;

    @Column(name = "planned_at")
    private LocalDateTime plannedAt;

    @Column(name = "last_refresh_requested_at")
    private LocalDateTime lastRefreshRequestedAt;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

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
