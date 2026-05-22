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
    private BigDecimal originalAmount;
    private BigDecimal discountAmount;
    private Long couponId;
    private Long buyerCouponId;
    private String couponName;
    private BigDecimal couponThresholdAmount;
    private BigDecimal couponDiscountAmount;
    private List<OrderCouponUsageResponse> couponUsages;
    private Long membershipPlanId;
    private String membershipPlanName;
    private BigDecimal membershipDiscountRate;
    private BigDecimal membershipDiscountAmount;
    private String address;
    private String receiverName;
    private String receiverPhone;
    private String receiverProvince;
    private String receiverCity;
    private String receiverDistrict;
    private String receiverDetailAddress;
    private String receiverFullAddress;
    private BigDecimal receiverLongitude;
    private BigDecimal receiverLatitude;
    private String receiverAddressValidationStatus;
    private String trackingNumber;
    private ShipmentResponse shipment;
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
        return from(o, buyerUsername, includeItems, null);
    }

    public static AdminOrderResponse from(Order o, String buyerUsername, boolean includeItems, ShipmentResponse shipment) {
        AdminOrderResponse r = new AdminOrderResponse();
        r.id = o.getId();
        r.buyerUsername = buyerUsername;
        r.status = o.getStatus().name();
        r.totalAmount = o.getTotalAmount();
        r.originalAmount = o.getOriginalAmount();
        r.discountAmount = o.getDiscountAmount();
        r.couponId = o.getCouponId();
        r.buyerCouponId = o.getBuyerCouponId();
        r.couponName = o.getCouponName();
        r.couponThresholdAmount = o.getCouponThresholdAmount();
        r.couponDiscountAmount = o.getCouponDiscountAmount();
        if (o.getCouponUsages() != null && !o.getCouponUsages().isEmpty()) {
            r.couponUsages = o.getCouponUsages().stream()
                    .map(OrderCouponUsageResponse::from)
                    .collect(Collectors.toList());
        }
        r.membershipPlanId = o.getMembershipPlanId();
        r.membershipPlanName = o.getMembershipPlanName();
        r.membershipDiscountRate = o.getMembershipDiscountRate();
        r.membershipDiscountAmount = o.getMembershipDiscountAmount();
        r.address = o.getReceiverFullAddress() != null ? o.getReceiverFullAddress() : o.getAddress();
        r.receiverName = o.getReceiverName();
        r.receiverPhone = o.getReceiverPhone();
        r.receiverProvince = o.getReceiverProvince();
        r.receiverCity = o.getReceiverCity();
        r.receiverDistrict = o.getReceiverDistrict();
        r.receiverDetailAddress = o.getReceiverDetailAddress();
        r.receiverFullAddress = o.getReceiverFullAddress() != null ? o.getReceiverFullAddress() : o.getAddress();
        r.receiverLongitude = o.getReceiverLongitude();
        r.receiverLatitude = o.getReceiverLatitude();
        r.receiverAddressValidationStatus = o.getReceiverAddressValidationStatus() != null ? o.getReceiverAddressValidationStatus().name() : null;
        r.trackingNumber = o.getTrackingNumber();
        r.shipment = shipment;
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
