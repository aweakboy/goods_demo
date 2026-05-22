package com.trading.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trading.common.BusinessException;
import com.trading.config.MapServiceConfig;
import com.trading.config.RoutePlanningConfig;
import com.trading.dto.RoutePlanningPoint;
import com.trading.dto.RoutePlanningRequest;
import com.trading.dto.RoutePlanningResult;
import com.trading.entity.Shipment;
import com.trading.entity.ShipmentRouteSnapshot;
import com.trading.enums.RoutePlanningStatus;
import com.trading.repository.ShipmentRepository;
import com.trading.repository.ShipmentRouteSnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ShipmentRoutePlanningService {

    private final ShipmentRouteSnapshotRepository routeSnapshotRepository;
    private final ShipmentRepository shipmentRepository;
    private final RoutePlanningClient routePlanningClient;
    private final MapServiceConfig mapConfig;
    private final RoutePlanningConfig routeConfig;
    private final ObjectMapper objectMapper;

    public Optional<ShipmentRouteSnapshot> findSnapshot(Long shipmentId) {
        if (shipmentId == null) {
            return Optional.empty();
        }
        return routeSnapshotRepository.findByShipmentId(shipmentId);
    }

    @Transactional
    public ShipmentRouteSnapshot planAfterShipmentCreated(Shipment shipment) {
        if (shipment == null || shipment.getId() == null) {
            return null;
        }
        if (!hasCoordinates(shipment)) {
            return saveSkipped(shipment, "发货地址或收货地址缺少经纬度，已使用模拟路线");
        }
        if (!isPlanningConfigured()) {
            return saveSkipped(shipment, "路径规划未启用或地图服务未配置，已使用模拟路线");
        }
        try {
            return saveAvailable(shipment, routePlanningClient.plan(toRequest(shipment)), LocalDateTime.now());
        } catch (RoutePlanningException e) {
            return saveFailed(shipment, e.getMessage(), LocalDateTime.now(), false);
        } catch (RuntimeException e) {
            return saveFailed(shipment, "路径规划暂不可用，请稍后重试", LocalDateTime.now(), false);
        }
    }

    @Transactional
    public ShipmentRouteSnapshot refreshRoute(Long shipmentId) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> BusinessException.notFound("发货记录不存在"));
        ShipmentRouteSnapshot existing = routeSnapshotRepository.findByShipmentId(shipmentId).orElse(null);
        LocalDateTime now = LocalDateTime.now();
        ensureRefreshAllowed(existing, now);

        if (!hasCoordinates(shipment)) {
            return saveSkipped(shipment, "发货地址或收货地址缺少经纬度，无法刷新真实路线", now);
        }
        if (!isPlanningConfigured()) {
            return saveSkipped(shipment, "路径规划未启用或地图服务未配置，无法刷新真实路线", now);
        }

        try {
            return saveAvailable(shipment, routePlanningClient.plan(toRequest(shipment)), now);
        } catch (RoutePlanningException e) {
            return saveFailed(shipment, e.getMessage(), now, true);
        } catch (RuntimeException e) {
            return saveFailed(shipment, "路径规划暂不可用，请稍后重试", now, true);
        }
    }

    public List<RoutePlanningPoint> parseRoutePath(ShipmentRouteSnapshot snapshot) {
        if (snapshot == null || snapshot.getRoutePolylineJson() == null || snapshot.getRoutePolylineJson().isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(snapshot.getRoutePolylineJson(), new TypeReference<List<RoutePlanningPoint>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    private ShipmentRouteSnapshot saveAvailable(Shipment shipment, RoutePlanningResult result, LocalDateTime now) {
        ShipmentRouteSnapshot snapshot = existingOrNew(shipment);
        snapshot.setProvider(firstNonBlank(result.getProvider(), provider()));
        snapshot.setPlanningStatus(RoutePlanningStatus.AVAILABLE);
        snapshot.setOriginLongitude(shipment.getOriginLongitude());
        snapshot.setOriginLatitude(shipment.getOriginLatitude());
        snapshot.setDestinationLongitude(shipment.getDestinationLongitude());
        snapshot.setDestinationLatitude(shipment.getDestinationLatitude());
        snapshot.setRoutePolylineJson(serializePath(result.getPath()));
        snapshot.setDistanceMeters(result.getDistanceMeters());
        snapshot.setDurationSeconds(result.getDurationSeconds());
        snapshot.setPlannedAt(now);
        snapshot.setLastRefreshRequestedAt(now);
        snapshot.setFailureReason(null);
        return routeSnapshotRepository.save(snapshot);
    }

    private ShipmentRouteSnapshot saveFailed(Shipment shipment, String reason, LocalDateTime now, boolean preserveAvailable) {
        ShipmentRouteSnapshot snapshot = existingOrNew(shipment);
        boolean keepAvailableRoute = preserveAvailable && snapshot.getPlanningStatus() == RoutePlanningStatus.AVAILABLE;
        snapshot.setProvider(firstNonBlank(snapshot.getProvider(), provider()));
        snapshot.setOriginLongitude(shipment.getOriginLongitude());
        snapshot.setOriginLatitude(shipment.getOriginLatitude());
        snapshot.setDestinationLongitude(shipment.getDestinationLongitude());
        snapshot.setDestinationLatitude(shipment.getDestinationLatitude());
        snapshot.setLastRefreshRequestedAt(now);
        snapshot.setFailureReason(limit(firstNonBlank(reason, "路径规划失败")));
        if (!keepAvailableRoute) {
            snapshot.setPlanningStatus(RoutePlanningStatus.FAILED);
            snapshot.setRoutePolylineJson(null);
            snapshot.setDistanceMeters(null);
            snapshot.setDurationSeconds(null);
            snapshot.setPlannedAt(null);
        }
        return routeSnapshotRepository.save(snapshot);
    }

    private ShipmentRouteSnapshot saveSkipped(Shipment shipment, String reason) {
        return saveSkipped(shipment, reason, LocalDateTime.now());
    }

    private ShipmentRouteSnapshot saveSkipped(Shipment shipment, String reason, LocalDateTime now) {
        ShipmentRouteSnapshot snapshot = existingOrNew(shipment);
        snapshot.setProvider(provider());
        snapshot.setPlanningStatus(RoutePlanningStatus.SKIPPED);
        snapshot.setOriginLongitude(shipment.getOriginLongitude());
        snapshot.setOriginLatitude(shipment.getOriginLatitude());
        snapshot.setDestinationLongitude(shipment.getDestinationLongitude());
        snapshot.setDestinationLatitude(shipment.getDestinationLatitude());
        snapshot.setRoutePolylineJson(null);
        snapshot.setDistanceMeters(null);
        snapshot.setDurationSeconds(null);
        snapshot.setPlannedAt(null);
        snapshot.setLastRefreshRequestedAt(now);
        snapshot.setFailureReason(limit(reason));
        return routeSnapshotRepository.save(snapshot);
    }

    private ShipmentRouteSnapshot existingOrNew(Shipment shipment) {
        return routeSnapshotRepository.findByShipmentId(shipment.getId())
                .orElseGet(() -> ShipmentRouteSnapshot.builder()
                        .shipmentId(shipment.getId())
                        .provider(provider())
                        .planningStatus(RoutePlanningStatus.SKIPPED)
                        .build());
    }

    private void ensureRefreshAllowed(ShipmentRouteSnapshot existing, LocalDateTime now) {
        if (existing == null || existing.getLastRefreshRequestedAt() == null) {
            return;
        }
        int minInterval = routeConfig.getMinRefreshIntervalMinutes();
        if (minInterval <= 0) {
            return;
        }
        long minutes = Duration.between(existing.getLastRefreshRequestedAt(), now).toMinutes();
        if (minutes < minInterval) {
            throw BusinessException.badRequest("路线刷新过于频繁，请稍后再试");
        }
    }

    private RoutePlanningRequest toRequest(Shipment shipment) {
        return RoutePlanningRequest.builder()
                .originLongitude(shipment.getOriginLongitude())
                .originLatitude(shipment.getOriginLatitude())
                .destinationLongitude(shipment.getDestinationLongitude())
                .destinationLatitude(shipment.getDestinationLatitude())
                .build();
    }

    private String serializePath(List<RoutePlanningPoint> path) {
        try {
            return objectMapper.writeValueAsString(path == null ? List.of() : path);
        } catch (Exception e) {
            throw new RoutePlanningException("路径规划暂不可用：路线坐标保存失败", e);
        }
    }

    private boolean hasCoordinates(Shipment shipment) {
        return shipment.getOriginLongitude() != null
                && shipment.getOriginLatitude() != null
                && shipment.getDestinationLongitude() != null
                && shipment.getDestinationLatitude() != null;
    }

    private boolean isPlanningConfigured() {
        return routeConfig.isEnabled()
                && mapConfig.isEnabled()
                && !isBlank(mapConfig.getWebServiceKey());
    }

    private String provider() {
        return firstNonBlank(routeConfig.getProvider(), "amap").toUpperCase();
    }

    private String firstNonBlank(String first, String second) {
        return isBlank(first) ? second : first;
    }

    private String limit(String value) {
        if (value == null || value.length() <= 500) {
            return value;
        }
        return value.substring(0, 500);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
