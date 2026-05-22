package com.trading.dto;

import com.trading.entity.Shop;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AdminShopResponse {
    private Long id;
    private String name;
    private String sellerUsername;
    private long productCount;
    private String status;
    private String province;
    private String city;
    private String district;
    private String fullAddress;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private String addressValidationStatus;
    private LocalDateTime createdAt;

    public static AdminShopResponse from(Shop s, String sellerUsername, long productCount) {
        AdminShopResponse r = new AdminShopResponse();
        r.id = s.getId();
        r.name = s.getName();
        r.sellerUsername = sellerUsername;
        r.productCount = productCount;
        r.status = s.getStatus().name();
        r.province = s.getProvince();
        r.city = s.getCity();
        r.district = s.getDistrict();
        r.fullAddress = s.getFullAddress();
        r.longitude = s.getLongitude();
        r.latitude = s.getLatitude();
        r.addressValidationStatus = s.getAddressValidationStatus() != null ? s.getAddressValidationStatus().name() : null;
        r.createdAt = s.getCreatedAt();
        return r;
    }
}
