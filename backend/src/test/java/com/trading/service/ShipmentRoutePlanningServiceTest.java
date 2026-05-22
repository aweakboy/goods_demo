package com.trading.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trading.common.BusinessException;
import com.trading.config.MapServiceConfig;
import com.trading.config.RoutePlanningConfig;
import com.trading.dto.RoutePlanningPoint;
import com.trading.dto.RoutePlanningResult;
import com.trading.entity.Shipment;
import com.trading.entity.ShipmentRouteSnapshot;
import com.trading.enums.RoutePlanningStatus;
import com.trading.repository.ShipmentRepository;
import com.trading.repository.ShipmentRouteSnapshotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ShipmentRoutePlanningServiceTest {

    @Mock ShipmentRouteSnapshotRepository routeSnapshotRepository;
    @Mock ShipmentRepository shipmentRepository;
    @Mock RoutePlanningClient routePlanningClient;

    private MapServiceConfig mapConfig;
    private RoutePlanningConfig routeConfig;
    private ShipmentRoutePlanningService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mapConfig = new MapServiceConfig();
        mapConfig.setEnabled(true);
        mapConfig.setWebServiceKey("test-key");
        routeConfig = new RoutePlanningConfig();
        routeConfig.setEnabled(true);
        routeConfig.setProvider("amap");
        routeConfig.setMinRefreshIntervalMinutes(10);
        service = new ShipmentRoutePlanningService(
                routeSnapshotRepository,
                shipmentRepository,
                routePlanningClient,
                mapConfig,
                routeConfig,
                new ObjectMapper()
        );
        when(routeSnapshotRepository.save(any(ShipmentRouteSnapshot.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void planAfterShipmentCreated_success_savesAvailableSnapshot() {
        Shipment shipment = shipment();
        when(routeSnapshotRepository.findByShipmentId(1L)).thenReturn(Optional.empty());
        when(routePlanningClient.plan(any())).thenReturn(routeResult());

        ShipmentRouteSnapshot snapshot = service.planAfterShipmentCreated(shipment);

        assertEquals(RoutePlanningStatus.AVAILABLE, snapshot.getPlanningStatus());
        assertEquals("AMAP", snapshot.getProvider());
        assertEquals(1000L, snapshot.getDistanceMeters());
        assertNotNull(snapshot.getRoutePolylineJson());
        verify(routePlanningClient).plan(any());
        verify(routeSnapshotRepository).save(snapshot);
    }

    @Test
    void planAfterShipmentCreated_missingCoordinates_skipsWithoutProviderCall() {
        Shipment shipment = shipment();
        shipment.setOriginLongitude(null);
        when(routeSnapshotRepository.findByShipmentId(1L)).thenReturn(Optional.empty());

        ShipmentRouteSnapshot snapshot = service.planAfterShipmentCreated(shipment);

        assertEquals(RoutePlanningStatus.SKIPPED, snapshot.getPlanningStatus());
        assertTrue(snapshot.getFailureReason().contains("缺少经纬度"));
        verify(routePlanningClient, never()).plan(any());
    }

    @Test
    void planAfterShipmentCreated_disabled_skipsWithoutProviderCall() {
        routeConfig.setEnabled(false);
        when(routeSnapshotRepository.findByShipmentId(1L)).thenReturn(Optional.empty());

        ShipmentRouteSnapshot snapshot = service.planAfterShipmentCreated(shipment());

        assertEquals(RoutePlanningStatus.SKIPPED, snapshot.getPlanningStatus());
        verify(routePlanningClient, never()).plan(any());
    }

    @Test
    void planAfterShipmentCreated_providerFailure_savesFailedSnapshotWithoutThrowing() {
        when(routeSnapshotRepository.findByShipmentId(1L)).thenReturn(Optional.empty());
        when(routePlanningClient.plan(any())).thenThrow(new RoutePlanningException("配额限制"));

        ShipmentRouteSnapshot snapshot = service.planAfterShipmentCreated(shipment());

        assertEquals(RoutePlanningStatus.FAILED, snapshot.getPlanningStatus());
        assertTrue(snapshot.getFailureReason().contains("配额限制"));
    }

    @Test
    void refreshRoute_tooFrequent_rejectsWithoutProviderCall() {
        ShipmentRouteSnapshot existing = ShipmentRouteSnapshot.builder()
                .shipmentId(1L)
                .planningStatus(RoutePlanningStatus.AVAILABLE)
                .lastRefreshRequestedAt(LocalDateTime.now())
                .build();
        when(shipmentRepository.findById(1L)).thenReturn(Optional.of(shipment()));
        when(routeSnapshotRepository.findByShipmentId(1L)).thenReturn(Optional.of(existing));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.refreshRoute(1L));

        assertEquals(400, ex.getStatus());
        verify(routePlanningClient, never()).plan(any());
    }

    @Test
    void refreshRoute_failurePreservesPreviousAvailableRoute() {
        ShipmentRouteSnapshot existing = ShipmentRouteSnapshot.builder()
                .shipmentId(1L)
                .provider("AMAP")
                .planningStatus(RoutePlanningStatus.AVAILABLE)
                .routePolylineJson("[{\"longitude\":120,\"latitude\":30},{\"longitude\":121,\"latitude\":31}]")
                .distanceMeters(1000L)
                .durationSeconds(600L)
                .plannedAt(LocalDateTime.now().minusHours(1))
                .lastRefreshRequestedAt(LocalDateTime.now().minusHours(1))
                .build();
        when(shipmentRepository.findById(1L)).thenReturn(Optional.of(shipment()));
        when(routeSnapshotRepository.findByShipmentId(1L)).thenReturn(Optional.of(existing));
        when(routePlanningClient.plan(any())).thenThrow(new RoutePlanningException("网络异常"));

        ShipmentRouteSnapshot snapshot = service.refreshRoute(1L);

        assertEquals(RoutePlanningStatus.AVAILABLE, snapshot.getPlanningStatus());
        assertEquals(1000L, snapshot.getDistanceMeters());
        assertTrue(snapshot.getFailureReason().contains("网络异常"));
    }

    private Shipment shipment() {
        return Shipment.builder()
                .id(1L)
                .originLongitude(new BigDecimal("120.0000000"))
                .originLatitude(new BigDecimal("30.0000000"))
                .destinationLongitude(new BigDecimal("121.0000000"))
                .destinationLatitude(new BigDecimal("31.0000000"))
                .build();
    }

    private RoutePlanningResult routeResult() {
        return RoutePlanningResult.builder()
                .provider("AMAP")
                .distanceMeters(1000L)
                .durationSeconds(600L)
                .path(List.of(
                        new RoutePlanningPoint(new BigDecimal("120.0000000"), new BigDecimal("30.0000000")),
                        new RoutePlanningPoint(new BigDecimal("120.5000000"), new BigDecimal("30.5000000")),
                        new RoutePlanningPoint(new BigDecimal("121.0000000"), new BigDecimal("31.0000000"))
                ))
                .build();
    }
}
