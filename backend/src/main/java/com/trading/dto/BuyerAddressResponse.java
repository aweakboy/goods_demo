package com.trading.dto;

import com.trading.entity.BuyerAddress;
import com.trading.enums.AddressValidationStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class BuyerAddressResponse {
    private Long id;
    private String receiverName;
    private String receiverPhone;
    private String province;
    private String city;
    private String district;
    private String detailAddress;
    private String fullAddress;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private String formattedAddress;
    private AddressValidationStatus validationStatus;
    private Boolean defaultAddress;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static BuyerAddressResponse from(BuyerAddress address) {
        return BuyerAddressResponse.builder()
                .id(address.getId())
                .receiverName(address.getReceiverName())
                .receiverPhone(address.getReceiverPhone())
                .province(address.getProvince())
                .city(address.getCity())
                .district(address.getDistrict())
                .detailAddress(address.getDetailAddress())
                .fullAddress(address.getFullAddress())
                .longitude(address.getLongitude())
                .latitude(address.getLatitude())
                .formattedAddress(address.getFormattedAddress())
                .validationStatus(address.getValidationStatus())
                .defaultAddress(address.getDefaultAddress())
                .createdAt(address.getCreatedAt())
                .updatedAt(address.getUpdatedAt())
                .build();
    }
}
