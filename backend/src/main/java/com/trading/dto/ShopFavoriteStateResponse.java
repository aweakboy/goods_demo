package com.trading.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ShopFavoriteStateResponse {
    private Long shopId;
    private Boolean favorited;
    private Long favoriteCount;
}
