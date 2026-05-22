package com.trading.dto;

import com.trading.entity.Shipment;
import com.trading.entity.ShipmentEvent;
import com.trading.enums.ShipmentStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Data
@Builder
public class ShipmentResponse {
    private Long id;
    private Long orderId;
    private String carrierCode;
    private String carrierName;
    private String trackingNumber;
    private String status;
    private String statusLabel;
    private boolean legacy;
    private boolean trackingAvailable;

    private String originName;
    private String originFullAddress;
    private BigDecimal originLongitude;
    private BigDecimal originLatitude;
    private String originAddressValidationStatus;

    private String destinationName;
    private String destinationPhone;
    private String destinationFullAddress;
    private BigDecimal destinationLongitude;
    private BigDecimal destinationLatitude;
    private String destinationAddressValidationStatus;

    private LocalDateTime shippedAt;
    private LocalDateTime estimatedDeliveredAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime latestEventTime;
    private String latestEventDescription;
    private List<ShipmentEventResponse> events;
    private ShipmentMapSimulationResponse mapSimulation;

    public static ShipmentResponse from(Shipment shipment, List<ShipmentEvent> events) {
        return from(shipment, events, null);
    }

    public static ShipmentResponse from(Shipment shipment, List<ShipmentEvent> events, ShipmentMapSimulationResponse mapSimulation) {
        List<ShipmentEventResponse> eventResponses = events.stream()
                .map(ShipmentEventResponse::from)
                .toList();
        ShipmentEvent latest = events.stream()
                .max(Comparator.comparing(ShipmentEvent::getEventTime).thenComparing(ShipmentEvent::getId))
                .orElse(null);
        return ShipmentResponse.builder()
                .id(shipment.getId())
                .orderId(shipment.getOrderId())
                .carrierCode(shipment.getCarrierCode())
                .carrierName(shipment.getCarrierName())
                .trackingNumber(shipment.getTrackingNumber())
                .status(shipment.getStatus() != null ? shipment.getStatus().name() : null)
                .statusLabel(statusLabel(shipment.getStatus()))
                .legacy(false)
                .trackingAvailable(true)
                .originName(shipment.getOriginName())
                .originFullAddress(shipment.getOriginFullAddress())
                .originLongitude(shipment.getOriginLongitude())
                .originLatitude(shipment.getOriginLatitude())
                .originAddressValidationStatus(shipment.getOriginAddressValidationStatus() != null ? shipment.getOriginAddressValidationStatus().name() : null)
                .destinationName(shipment.getDestinationName())
                .destinationPhone(shipment.getDestinationPhone())
                .destinationFullAddress(shipment.getDestinationFullAddress())
                .destinationLongitude(shipment.getDestinationLongitude())
                .destinationLatitude(shipment.getDestinationLatitude())
                .destinationAddressValidationStatus(shipment.getDestinationAddressValidationStatus() != null ? shipment.getDestinationAddressValidationStatus().name() : null)
                .shippedAt(shipment.getShippedAt())
                .estimatedDeliveredAt(shipment.getEstimatedDeliveredAt())
                .deliveredAt(shipment.getDeliveredAt())
                .latestEventTime(latest != null ? latest.getEventTime() : null)
                .latestEventDescription(latest != null ? latest.getDescription() : null)
                .events(eventResponses)
                .mapSimulation(mapSimulation)
                .build();
    }

    public static ShipmentResponse legacy(Long orderId, String trackingNumber) {
        return ShipmentResponse.builder()
                .orderId(orderId)
                .trackingNumber(trackingNumber)
                .status("SHIPPED")
                .statusLabel("已发货")
                .legacy(true)
                .trackingAvailable(false)
                .latestEventDescription("历史物流单号，暂无结构化轨迹")
                .events(List.of())
                .mapSimulation(ShipmentMapSimulationResponse.unavailable("历史物流单号暂无地图路线"))
                .build();
    }

    public static String statusLabel(ShipmentStatus status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case SHIPPED -> "已揽收";
            case IN_TRANSIT -> "运输中";
            case OUT_FOR_DELIVERY -> "派送中";
            case DELIVERED -> "已签收";
            case EXCEPTION -> "物流异常";
        };
    }
}
