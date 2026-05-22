package com.trading.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoutePlanningResult {
    private String provider;
    private List<RoutePlanningPoint> path;
    private Long distanceMeters;
    private Long durationSeconds;
}
