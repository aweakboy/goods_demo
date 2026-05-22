package com.trading.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ShipmentMapPointResponse {
    private BigDecimal longitude;
    private BigDecimal latitude;
    private String title;
    private String address;
}
