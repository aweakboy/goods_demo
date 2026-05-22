package com.trading.dto;

import com.trading.entity.ShipmentEvent;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ShipmentEventResponse {
    private Long id;
    private String status;
    private String statusLabel;
    private LocalDateTime eventTime;
    private String location;
    private String description;

    public static ShipmentEventResponse from(ShipmentEvent event) {
        return ShipmentEventResponse.builder()
                .id(event.getId())
                .status(event.getStatus() != null ? event.getStatus().name() : null)
                .statusLabel(ShipmentResponse.statusLabel(event.getStatus()))
                .eventTime(event.getEventTime())
                .location(event.getLocation())
                .description(event.getDescription())
                .build();
    }
}
