package com.trading.service;

import com.trading.dto.ShipmentMapSimulationResponse;
import com.trading.entity.Shipment;
import com.trading.entity.ShipmentRouteSnapshot;
import com.trading.enums.RoutePlanningStatus;
import com.trading.enums.ShipmentStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ShipmentMapSimulationServiceTest {

    private final ShipmentMapSimulationService service = new ShipmentMapSimulationService();

    @Test
    void build_completeCoordinates_returnsRouteAndCurrentPosition() {
        Shipment shipment = baseShipment(ShipmentStatus.IN_TRANSIT);

        ShipmentMapSimulationResponse response = service.build(shipment);

        assertTrue(response.isRouteAvailable());
        assertEquals(new BigDecimal("0.50"), response.getProgress());
        assertEquals(new BigDecimal("120.5000000"), response.getCurrentPosition().getLongitude());
        assertEquals(new BigDecimal("30.5000000"), response.getCurrentPosition().getLatitude());
        assertEquals("运输中", response.getStatusLabel());
    }

    @Test
    void build_missingOriginCoordinate_returnsFallback() {
        Shipment shipment = baseShipment(ShipmentStatus.SHIPPED);
        shipment.setOriginLongitude(null);

        ShipmentMapSimulationResponse response = service.build(shipment);

        assertFalse(response.isRouteAvailable());
        assertTrue(response.getFallbackReason().contains("发货地址缺少经纬度"));
        assertNull(response.getCurrentPosition());
    }

    @Test
    void build_missingDestinationCoordinate_returnsFallback() {
        Shipment shipment = baseShipment(ShipmentStatus.SHIPPED);
        shipment.setDestinationLatitude(null);

        ShipmentMapSimulationResponse response = service.build(shipment);

        assertFalse(response.isRouteAvailable());
        assertTrue(response.getFallbackReason().contains("收货地址缺少经纬度"));
    }

    @Test
    void build_statusProgressMapping_isStable() {
        assertEquals(new BigDecimal("0.05"), service.build(baseShipment(ShipmentStatus.SHIPPED)).getProgress());
        assertEquals(new BigDecimal("0.50"), service.build(baseShipment(ShipmentStatus.IN_TRANSIT)).getProgress());
        assertEquals(new BigDecimal("0.85"), service.build(baseShipment(ShipmentStatus.OUT_FOR_DELIVERY)).getProgress());
        assertEquals(BigDecimal.ONE, service.build(baseShipment(ShipmentStatus.DELIVERED)).getProgress());
    }

    @Test
    void build_exception_usesFallbackProgressAndExceptionDescription() {
        ShipmentMapSimulationResponse response = service.build(baseShipment(ShipmentStatus.EXCEPTION));

        assertTrue(response.isRouteAvailable());
        assertEquals(new BigDecimal("0.50"), response.getProgress());
        assertEquals("物流异常", response.getStatusLabel());
        assertTrue(response.getCurrentDescription().contains("异常"));
    }

    @Test
    void build_availableRouteSnapshot_returnsPlannedRouteAndCompletedPath() {
        ShipmentRouteSnapshot snapshot = ShipmentRouteSnapshot.builder()
                .shipmentId(1L)
                .provider("AMAP")
                .planningStatus(RoutePlanningStatus.AVAILABLE)
                .routePolylineJson("""
                        [
                          {"longitude":120.0000000,"latitude":30.0000000},
                          {"longitude":120.5000000,"latitude":30.5000000},
                          {"longitude":121.0000000,"latitude":31.0000000}
                        ]
                        """)
                .distanceMeters(1000L)
                .durationSeconds(600L)
                .plannedAt(LocalDateTime.now())
                .build();

        ShipmentMapSimulationResponse response = service.build(baseShipment(ShipmentStatus.IN_TRANSIT), snapshot);

        assertTrue(response.isRouteAvailable());
        assertEquals("PLANNED", response.getRouteSource());
        assertEquals(3, response.getRoutePath().size());
        assertTrue(response.getCompletedPath().size() >= 2);
        assertEquals(1000L, response.getDistanceMeters());
        assertEquals(600L, response.getDurationSeconds());
        assertEquals(new BigDecimal("0.50"), response.getProgress());
    }

    @Test
    void legacyShipmentResponse_hasUnavailableMapRoute() {
        var response = com.trading.dto.ShipmentResponse.legacy(1L, "OLD100");

        assertTrue(response.isLegacy());
        assertNotNull(response.getMapSimulation());
        assertFalse(response.getMapSimulation().isRouteAvailable());
    }

    private Shipment baseShipment(ShipmentStatus status) {
        return Shipment.builder()
                .id(1L)
                .orderId(100L)
                .status(status)
                .originName("测试店铺")
                .originFullAddress("发货地")
                .originLongitude(new BigDecimal("120.0000000"))
                .originLatitude(new BigDecimal("30.0000000"))
                .destinationName("张三")
                .destinationFullAddress("收货地")
                .destinationLongitude(new BigDecimal("121.0000000"))
                .destinationLatitude(new BigDecimal("31.0000000"))
                .build();
    }
}
