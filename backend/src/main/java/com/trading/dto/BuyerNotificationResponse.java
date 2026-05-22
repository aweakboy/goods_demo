package com.trading.dto;

import com.trading.entity.BuyerNotification;
import com.trading.entity.Product;
import com.trading.entity.Shop;
import com.trading.enums.BuyerNotificationType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class BuyerNotificationResponse {
    private Long id;
    private BuyerNotificationType type;
    private String title;
    private String content;
    private Long productId;
    private String productName;
    private Long shopId;
    private String shopName;
    private Boolean read;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;

    public static BuyerNotificationResponse from(BuyerNotification notification) {
        Product product = notification.getProduct();
        Shop shop = notification.getShop();
        return BuyerNotificationResponse.builder()
                .id(notification.getId())
                .type(notification.getType())
                .title(notification.getTitle())
                .content(notification.getContent())
                .productId(notification.getProductId())
                .productName(product != null ? product.getName() : null)
                .shopId(notification.getShopId())
                .shopName(shop != null ? shop.getName() : null)
                .read(notification.getReadAt() != null)
                .readAt(notification.getReadAt())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
