package com.trading.dto;

import com.trading.entity.Shop;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ShopResponse {
    private Long id;
    private Long sellerId;
    private String name;
    private String description;
    private String status;
    private LocalDateTime createdAt;

    public static ShopResponse from(Shop s) {
        ShopResponse r = new ShopResponse();
        r.id = s.getId();
        r.sellerId = s.getSellerId();
        r.name = s.getName();
        r.description = s.getDescription();
        r.status = s.getStatus().name();
        r.createdAt = s.getCreatedAt();
        return r;
    }
}
