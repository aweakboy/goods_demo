package com.trading.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddressValidationRequest {
    private String province;
    private String city;
    private String district;
    private String detailAddress;
}
