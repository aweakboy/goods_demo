package com.trading.dto;

import com.trading.entity.Shop;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminShopResponse {
    private Long id;
    private String name;
    private String sellerUsername;
    private long productCount;
    private String status;
    private LocalDateTime createdAt;

    public static AdminShopResponse from(Shop s, String sellerUsername, long productCount) {
        AdminShopResponse r = new AdminShopResponse();
        r.id = s.getId();
        r.name = s.getName();
        r.sellerUsername = sellerUsername;
        r.productCount = productCount;
        r.status = s.getStatus().name();
        r.createdAt = s.getCreatedAt();
        return r;
    }
}
