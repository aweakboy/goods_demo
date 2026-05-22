package com.trading.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trading.dto.RoutePlanningPoint;
import com.trading.dto.ShipmentMapPointResponse;
import com.trading.dto.ShipmentMapSimulationResponse;
import com.trading.dto.ShipmentResponse;
import com.trading.entity.ShipmentRouteSnapshot;
import com.trading.entity.Shipment;
import com.trading.enums.RoutePlanningStatus;
import com.trading.enums.RouteSource;
import com.trading.enums.ShipmentStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ShipmentMapSimulationService {

    private static final BigDecimal PROGRESS_SHIPPED = new BigDecimal("0.05");
    private static final BigDecimal PROGRESS_IN_TRANSIT = new BigDecimal("0.50");
    private static final BigDecimal PROGRESS_OUT_FOR_DELIVERY = new BigDecimal("0.85");
    private static final BigDecimal PROGRESS_DELIVERED = BigDecimal.ONE;
    private static final BigDecimal PROGRESS_EXCEPTION_FALLBACK = new BigDecimal("0.50");

    private final ObjectMapper objectMapper;

    public ShipmentMapSimulationService() {
        this(new ObjectMapper());
    }

    @Autowired
    public ShipmentMapSimulationService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ShipmentMapSimulationResponse build(Shipment shipment) {
        return build(shipment, null);
    }

    public ShipmentMapSimulationResponse build(Shipment shipment, ShipmentRouteSnapshot routeSnapshot) {
        if (shipment == null) {
            return ShipmentMapSimulationResponse.unavailable("暂无发货记录，无法展示物流地图");
        }
        if (!hasCoordinate(shipment.getOriginLongitude(), shipment.getOriginLatitude())) {
            return ShipmentMapSimulationResponse.unavailable("发货地址缺少经纬度，无法展示物流地图");
        }
        if (!hasCoordinate(shipment.getDestinationLongitude(), shipment.getDestinationLatitude())) {
            return ShipmentMapSimulationResponse.unavailable("收货地址缺少经纬度，无法展示物流地图");
        }
        ShipmentMapSimulationResponse planned = buildPlanned(shipment, routeSnapshot);
        if (planned != null) {
            return planned;
        }

        BigDecimal progress = progressOf(shipment.getStatus());
        ShipmentMapPointResponse origin = ShipmentMapPointResponse.builder()
                .longitude(shipment.getOriginLongitude())
                .latitude(shipment.getOriginLatitude())
                .title("发货地")
                .address(shipment.getOriginFullAddress())
                .build();
        ShipmentMapPointResponse destination = ShipmentMapPointResponse.builder()
                .longitude(shipment.getDestinationLongitude())
                .latitude(shipment.getDestinationLatitude())
                .title("收货地")
                .address(shipment.getDestinationFullAddress())
                .build();
        ShipmentMapPointResponse current = ShipmentMapPointResponse.builder()
                .longitude(interpolate(shipment.getOriginLongitude(), shipment.getDestinationLongitude(), progress))
                .latitude(interpolate(shipment.getOriginLatitude(), shipment.getDestinationLatitude(), progress))
                .title(currentTitle(shipment.getStatus()))
                .address(currentDescription(shipment.getStatus()))
                .build();
        List<ShipmentMapPointResponse> routePath = List.of(origin, destination);
        List<ShipmentMapPointResponse> completedPath = List.of(origin, current);

        return ShipmentMapSimulationResponse.builder()
                .origin(origin)
                .destination(destination)
                .currentPosition(current)
                .progress(progress)
                .routeAvailable(true)
                .routeSource(RouteSource.SIMULATED.name())
                .routePath(routePath)
                .completedPath(completedPath)
                .planningStatus(routeSnapshot != null && routeSnapshot.getPlanningStatus() != null
                        ? routeSnapshot.getPlanningStatus().name()
                        : null)
                .planningFailureReason(routeSnapshot != null ? routeSnapshot.getFailureReason() : null)
                .status(shipment.getStatus() != null ? shipment.getStatus().name() : null)
                .statusLabel(ShipmentResponse.statusLabel(shipment.getStatus()))
                .currentDescription(currentDescription(shipment.getStatus()))
                .build();
    }

    private ShipmentMapSimulationResponse buildPlanned(Shipment shipment, ShipmentRouteSnapshot routeSnapshot) {
        if (routeSnapshot == null || routeSnapshot.getPlanningStatus() != RoutePlanningStatus.AVAILABLE) {
            return null;
        }
        List<ShipmentMapPointResponse> routePath = parseRoutePath(routeSnapshot);
        if (routePath.size() < 2) {
            return null;
        }

        BigDecimal progress = progressOf(shipment.getStatus());
        PositionOnRoute position = positionOnRoute(routePath, progress);
        ShipmentMapPointResponse origin = routePath.get(0);
        origin.setTitle("发货地");
        origin.setAddress(shipment.getOriginFullAddress());
        ShipmentMapPointResponse destination = routePath.get(routePath.size() - 1);
        destination.setTitle("收货地");
        destination.setAddress(shipment.getDestinationFullAddress());
        ShipmentMapPointResponse current = ShipmentMapPointResponse.builder()
                .longitude(position.current().getLongitude())
                .latitude(position.current().getLatitude())
                .title(currentTitle(shipment.getStatus()))
                .address(currentDescription(shipment.getStatus()))
                .build();

        return ShipmentMapSimulationResponse.builder()
                .origin(origin)
                .destination(destination)
                .currentPosition(current)
                .progress(progress)
                .routeAvailable(true)
                .routeSource(RouteSource.PLANNED.name())
                .routePath(routePath)
                .completedPath(position.completedPath())
                .distanceMeters(routeSnapshot.getDistanceMeters())
                .durationSeconds(routeSnapshot.getDurationSeconds())
                .plannedAt(routeSnapshot.getPlannedAt())
                .planningStatus(routeSnapshot.getPlanningStatus().name())
                .planningFailureReason(routeSnapshot.getFailureReason())
                .status(shipment.getStatus() != null ? shipment.getStatus().name() : null)
                .statusLabel(ShipmentResponse.statusLabel(shipment.getStatus()))
                .currentDescription(currentDescription(shipment.getStatus()))
                .build();
    }

    private List<ShipmentMapPointResponse> parseRoutePath(ShipmentRouteSnapshot routeSnapshot) {
        try {
            List<RoutePlanningPoint> points = objectMapper.readValue(
                    routeSnapshot.getRoutePolylineJson(),
                    new TypeReference<List<RoutePlanningPoint>>() {}
            );
            if (points == null) {
                return List.of();
            }
            return points.stream()
                    .map(point -> ShipmentMapPointResponse.builder()
                            .longitude(point.getLongitude())
                            .latitude(point.getLatitude())
                            .build())
                    .toList();
        } catch (Exception e) {
            return List.of();
        }
    }

    private PositionOnRoute positionOnRoute(List<ShipmentMapPointResponse> path, BigDecimal progress) {
        if (progress.compareTo(BigDecimal.ONE) >= 0) {
            return new PositionOnRoute(path.get(path.size() - 1), new ArrayList<>(path));
        }
        if (progress.compareTo(BigDecimal.ZERO) <= 0) {
            return new PositionOnRoute(path.get(0), List.of(path.get(0)));
        }

        double total = 0;
        List<Double> segmentDistances = new ArrayList<>();
        for (int i = 0; i < path.size() - 1; i++) {
            double distance = distance(path.get(i), path.get(i + 1));
            segmentDistances.add(distance);
            total += distance;
        }
        if (total <= 0) {
            return new PositionOnRoute(path.get(0), List.of(path.get(0)));
        }

        double target = total * progress.doubleValue();
        double walked = 0;
        List<ShipmentMapPointResponse> completed = new ArrayList<>();
        completed.add(path.get(0));
        for (int i = 0; i < segmentDistances.size(); i++) {
            ShipmentMapPointResponse start = path.get(i);
            ShipmentMapPointResponse end = path.get(i + 1);
            double segment = segmentDistances.get(i);
            if (walked + segment >= target) {
                double ratio = segment == 0 ? 0 : (target - walked) / segment;
                ShipmentMapPointResponse current = interpolatePoint(start, end, ratio);
                if (!samePoint(completed.get(completed.size() - 1), current)) {
                    completed.add(current);
                }
                return new PositionOnRoute(current, completed);
            }
            completed.add(end);
            walked += segment;
        }
        ShipmentMapPointResponse last = path.get(path.size() - 1);
        return new PositionOnRoute(last, completed);
    }

    private ShipmentMapPointResponse interpolatePoint(ShipmentMapPointResponse start, ShipmentMapPointResponse end, double ratio) {
        BigDecimal progress = BigDecimal.valueOf(ratio);
        return ShipmentMapPointResponse.builder()
                .longitude(interpolate(start.getLongitude(), end.getLongitude(), progress))
                .latitude(interpolate(start.getLatitude(), end.getLatitude(), progress))
                .build();
    }

    private double distance(ShipmentMapPointResponse start, ShipmentMapPointResponse end) {
        double earthRadius = 6371000;
        double lat1 = Math.toRadians(start.getLatitude().doubleValue());
        double lat2 = Math.toRadians(end.getLatitude().doubleValue());
        double dLat = lat2 - lat1;
        double dLng = Math.toRadians(end.getLongitude().doubleValue() - start.getLongitude().doubleValue());
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return earthRadius * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private boolean samePoint(ShipmentMapPointResponse left, ShipmentMapPointResponse right) {
        return left.getLongitude().compareTo(right.getLongitude()) == 0
                && left.getLatitude().compareTo(right.getLatitude()) == 0;
    }

    public BigDecimal progressOf(ShipmentStatus status) {
        if (status == null) {
            return PROGRESS_SHIPPED;
        }
        return switch (status) {
            case SHIPPED -> PROGRESS_SHIPPED;
            case IN_TRANSIT -> PROGRESS_IN_TRANSIT;
            case OUT_FOR_DELIVERY -> PROGRESS_OUT_FOR_DELIVERY;
            case DELIVERED -> PROGRESS_DELIVERED;
            case EXCEPTION -> PROGRESS_EXCEPTION_FALLBACK;
        };
    }

    private String currentTitle(ShipmentStatus status) {
        return switch (status == null ? ShipmentStatus.SHIPPED : status) {
            case SHIPPED -> "已揽收";
            case IN_TRANSIT -> "运输中";
            case OUT_FOR_DELIVERY -> "派送中";
            case DELIVERED -> "已签收";
            case EXCEPTION -> "物流异常";
        };
    }

    private String currentDescription(ShipmentStatus status) {
        return switch (status == null ? ShipmentStatus.SHIPPED : status) {
            case SHIPPED -> "包裹已从发货地揽收";
            case IN_TRANSIT -> "包裹正在发货地与收货地之间运输";
            case OUT_FOR_DELIVERY -> "包裹已接近收货地，正在派送";
            case DELIVERED -> "包裹已到达收货地并签收";
            case EXCEPTION -> "物流异常，显示最近模拟位置";
        };
    }

    private BigDecimal interpolate(BigDecimal start, BigDecimal end, BigDecimal progress) {
        return start.add(end.subtract(start).multiply(progress)).setScale(7, RoundingMode.HALF_UP);
    }

    private boolean hasCoordinate(BigDecimal longitude, BigDecimal latitude) {
        return longitude != null && latitude != null;
    }

    private record PositionOnRoute(ShipmentMapPointResponse current, List<ShipmentMapPointResponse> completedPath) {}
}
