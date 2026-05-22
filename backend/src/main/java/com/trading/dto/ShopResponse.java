package com.trading.dto;

import com.trading.entity.Shop;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ShopResponse {
    private Long id;
    private Long sellerId;
    private String name;
    private String description;
    private String status;
    private String province;
    private String city;
    private String district;
    private String detailAddress;
    private String fullAddress;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private String addressValidationStatus;
    private LocalDateTime createdAt;
    private Boolean favorited;
    private Long favoriteCount;

    public static ShopResponse from(Shop s) {
        ShopResponse r = new ShopResponse();
        r.id = s.getId();
        r.sellerId = s.getSellerId();
        r.name = s.getName();
        r.description = s.getDescription();
        r.status = s.getStatus().name();
        r.province = s.getProvince();
        r.city = s.getCity();
        r.district = s.getDistrict();
        r.detailAddress = s.getDetailAddress();
        r.fullAddress = s.getFullAddress();
        r.longitude = s.getLongitude();
        r.latitude = s.getLatitude();
        r.addressValidationStatus = s.getAddressValidationStatus() != null ? s.getAddressValidationStatus().name() : null;
        r.createdAt = s.getCreatedAt();
        return r;
    }
}
