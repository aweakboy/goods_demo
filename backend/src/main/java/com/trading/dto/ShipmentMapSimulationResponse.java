package com.trading.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ShipmentMapSimulationResponse {
    private ShipmentMapPointResponse origin;
    private ShipmentMapPointResponse destination;
    private ShipmentMapPointResponse currentPosition;
    private BigDecimal progress;
    private boolean routeAvailable;
    private String fallbackReason;
    private String status;
    private String statusLabel;
    private String currentDescription;
    private String routeSource;
    private List<ShipmentMapPointResponse> routePath;
    private List<ShipmentMapPointResponse> completedPath;
    private Long distanceMeters;
    private Long durationSeconds;
    private LocalDateTime plannedAt;
    private String planningStatus;
    private String planningFailureReason;

    public static ShipmentMapSimulationResponse unavailable(String reason) {
        return ShipmentMapSimulationResponse.builder()
                .routeAvailable(false)
                .fallbackReason(reason)
                .build();
    }
}
