package com.trading.dto;

import com.trading.entity.Order;
import com.trading.entity.OrderItem;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class AdminOrderResponse {
    private Long id;
    private String buyerUsername;
    private String status;
    private BigDecimal totalAmount;
    private String address;
    private String trackingNumber;
    private LocalDateTime createdAt;
    private String productSummary;
    private List<ItemDto> items;

    @Data
    public static class ItemDto {
        private String productName;
        private BigDecimal price;
        private Integer quantity;
    }

    public static AdminOrderResponse from(Order o, String buyerUsername, boolean includeItems) {
        AdminOrderResponse r = new AdminOrderResponse();
        r.id = o.getId();
        r.buyerUsername = buyerUsername;
        r.status = o.getStatus().name();
        r.totalAmount = o.getTotalAmount();
        r.address = o.getAddress();
        r.trackingNumber = o.getTrackingNumber();
        r.createdAt = o.getCreatedAt();
        if (o.getItems() != null && !o.getItems().isEmpty()) {
            List<String> names = o.getItems().stream().map(OrderItem::getProductName).collect(Collectors.toList());
            r.productSummary = names.size() > 2
                    ? names.get(0) + "、" + names.get(1) + " 等" + names.size() + "件"
                    : String.join("、", names);
            if (includeItems) {
                r.items = o.getItems().stream().map(i -> {
                    ItemDto d = new ItemDto();
                    d.productName = i.getProductName();
                    d.price = i.getPrice();
                    d.quantity = i.getQuantity();
                    return d;
                }).collect(Collectors.toList());
            }
        }
        return r;
    }
}
