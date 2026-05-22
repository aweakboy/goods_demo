package com.trading.dto;

import com.trading.entity.PriceAlert;
import com.trading.entity.Product;
import com.trading.enums.PriceAlertStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class PriceAlertResponse {
    private Long id;
    private Long productId;
    private String productName;
    private BigDecimal currentPrice;
    private String imageUrl;
    private BigDecimal targetPrice;
    private PriceAlertStatus status;
    private BigDecimal lastNotifiedPrice;
    private LocalDateTime triggeredAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static PriceAlertResponse from(PriceAlert alert) {
        Product product = alert.getProduct();
        return PriceAlertResponse.builder()
                .id(alert.getId())
                .productId(alert.getProductId())
                .productName(product != null ? product.getName() : null)
                .currentPrice(product != null ? product.getPrice() : null)
                .imageUrl(product != null ? product.getImageUrl() : null)
                .targetPrice(alert.getTargetPrice())
                .status(alert.getStatus())
                .lastNotifiedPrice(alert.getLastNotifiedPrice())
                .triggeredAt(alert.getTriggeredAt())
                .createdAt(alert.getCreatedAt())
                .updatedAt(alert.getUpdatedAt())
                .build();
    }
}
