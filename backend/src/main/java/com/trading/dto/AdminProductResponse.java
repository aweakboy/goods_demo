package com.trading.dto;

import com.trading.entity.Product;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AdminProductResponse {
    private Long id;
    private String name;
    private BigDecimal price;
    private Integer stock;
    private String status;
    private String sellerUsername;
    private String shopName;
    private Long categoryId;
    private LocalDateTime createdAt;

    public static AdminProductResponse from(Product p, String sellerUsername, String shopName) {
        AdminProductResponse r = new AdminProductResponse();
        r.id = p.getId();
        r.name = p.getName();
        r.price = p.getPrice();
        r.stock = p.getStock();
        r.status = p.getStatus().name();
        r.sellerUsername = sellerUsername;
        r.shopName = shopName;
        r.categoryId = p.getCategoryId();
        r.createdAt = p.getCreatedAt();
        return r;
    }
}
