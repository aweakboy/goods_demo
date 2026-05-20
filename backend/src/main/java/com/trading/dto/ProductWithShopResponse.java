package com.trading.dto;

import com.trading.entity.Product;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ProductWithShopResponse {
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer stock;
    private String status;
    private String imageUrl;
    private Long sellerId;
    private Long categoryId;
    private LocalDateTime createdAt;
    private Long shopId;
    private String shopName;

    public static ProductWithShopResponse from(Product p, Long shopId, String shopName) {
        ProductWithShopResponse r = new ProductWithShopResponse();
        r.id = p.getId();
        r.name = p.getName();
        r.description = p.getDescription();
        r.price = p.getPrice();
        r.stock = p.getStock();
        r.status = p.getStatus().name();
        r.imageUrl = p.getImageUrl();
        r.sellerId = p.getSellerId();
        r.categoryId = p.getCategoryId();
        r.createdAt = p.getCreatedAt();
        r.shopId = shopId;
        r.shopName = shopName;
        return r;
    }
}
