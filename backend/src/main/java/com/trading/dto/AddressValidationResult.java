package com.trading.dto;

import com.trading.enums.AddressValidationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressValidationResult {
    private AddressValidationStatus status;
    private String formattedAddress;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private String province;
    private String city;
    private String district;
    private String adcode;
    private String level;
    private String message;
}
