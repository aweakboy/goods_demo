package com.trading.repository;

import com.trading.entity.ShipmentRouteSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ShipmentRouteSnapshotRepository extends JpaRepository<ShipmentRouteSnapshot, Long> {
    Optional<ShipmentRouteSnapshot> findByShipmentId(Long shipmentId);
}
