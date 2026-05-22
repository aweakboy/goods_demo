package com.trading.dto;

import com.trading.entity.Shop;
import com.trading.entity.ShopFavorite;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ShopFavoriteResponse {
    private Long id;
    private Long shopId;
    private String shopName;
    private String shopDescription;
    private String shopStatus;
    private Boolean accessible;
    private LocalDateTime createdAt;

    public static ShopFavoriteResponse from(ShopFavorite favorite) {
        Shop shop = favorite.getShop();
        String status = shop != null && shop.getStatus() != null ? shop.getStatus().name() : null;
        return ShopFavoriteResponse.builder()
                .id(favorite.getId())
                .shopId(favorite.getShopId())
                .shopName(shop != null ? shop.getName() : null)
                .shopDescription(shop != null ? shop.getDescription() : null)
                .shopStatus(status)
                .accessible("ACTIVE".equals(status))
                .createdAt(favorite.getCreatedAt())
                .build();
    }
}
