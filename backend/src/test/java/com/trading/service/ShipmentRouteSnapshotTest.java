package com.trading.service;

import com.trading.entity.ShipmentRouteSnapshot;
import com.trading.enums.RoutePlanningStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ShipmentRouteSnapshotTest {

    @Test
    void snapshot_canRepresentCreateUpdateAndReadState() {
        ShipmentRouteSnapshot snapshot = ShipmentRouteSnapshot.builder()
                .shipmentId(1L)
                .provider("AMAP")
                .planningStatus(RoutePlanningStatus.SKIPPED)
                .failureReason("未配置")
                .build();

        assertEquals(1L, snapshot.getShipmentId());
        assertEquals(RoutePlanningStatus.SKIPPED, snapshot.getPlanningStatus());

        LocalDateTime plannedAt = LocalDateTime.now();
        snapshot.setPlanningStatus(RoutePlanningStatus.AVAILABLE);
        snapshot.setRoutePolylineJson("[{\"longitude\":120,\"latitude\":30},{\"longitude\":121,\"latitude\":31}]");
        snapshot.setDistanceMeters(1000L);
        snapshot.setDurationSeconds(600L);
        snapshot.setPlannedAt(plannedAt);
        snapshot.setFailureReason(null);

        assertEquals(RoutePlanningStatus.AVAILABLE, snapshot.getPlanningStatus());
        assertEquals(1000L, snapshot.getDistanceMeters());
        assertEquals(600L, snapshot.getDurationSeconds());
        assertEquals(plannedAt, snapshot.getPlannedAt());
        assertNull(snapshot.getFailureReason());
    }
}
