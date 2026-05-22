package com.trading.dto;

import lombok.Data;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ShopStorefrontResponse {
    private Long id;
    private String name;
    private String description;
    private String status;
    private String province;
    private String city;
    private String district;
    private String fullAddress;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private String addressValidationStatus;
    private LocalDateTime createdAt;
    private Boolean favorited;
    private Long favoriteCount;
    private Page<ProductWithShopResponse> products;
}
