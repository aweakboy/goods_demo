package com.trading.entity;

import com.trading.enums.OrderStatus;
import com.trading.enums.AddressValidationStatus;
import com.trading.dto.ShipmentResponse;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Order {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "buyer_id", nullable = false)
    private Long buyerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status = OrderStatus.PENDING_PAYMENT;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "original_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal originalAmount;

    @Builder.Default
    @Column(name = "discount_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "coupon_id")
    private Long couponId;

    @Column(name = "buyer_coupon_id")
    private Long buyerCouponId;

    @Column(name = "coupon_name", length = 100)
    private String couponName;

    @Column(name = "coupon_threshold_amount", precision = 12, scale = 2)
    private BigDecimal couponThresholdAmount;

    @Column(name = "coupon_discount_amount", precision = 12, scale = 2)
    private BigDecimal couponDiscountAmount;

    @Column(name = "membership_plan_id")
    private Long membershipPlanId;

    @Column(name = "membership_plan_name", length = 100)
    private String membershipPlanName;

    @Column(name = "membership_discount_rate", precision = 5, scale = 4)
    private BigDecimal membershipDiscountRate;

    @Builder.Default
    @Column(name = "membership_discount_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal membershipDiscountAmount = BigDecimal.ZERO;

    @Column(nullable = false, length = 500)
    private String address;

    @Column(name = "receiver_name", length = 50)
    private String receiverName;

    @Column(name = "receiver_phone", length = 30)
    private String receiverPhone;

    @Column(name = "receiver_province", length = 50)
    private String receiverProvince;

    @Column(name = "receiver_city", length = 50)
    private String receiverCity;

    @Column(name = "receiver_district", length = 50)
    private String receiverDistrict;

    @Column(name = "receiver_detail_address", length = 300)
    private String receiverDetailAddress;

    @Column(name = "receiver_full_address", length = 500)
    private String receiverFullAddress;

    @Column(name = "receiver_longitude", precision = 10, scale = 7)
    private BigDecimal receiverLongitude;

    @Column(name = "receiver_latitude", precision = 10, scale = 7)
    private BigDecimal receiverLatitude;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "receiver_address_validation_status", nullable = false, length = 20)
    private AddressValidationStatus receiverAddressValidationStatus = AddressValidationStatus.UNVERIFIED;

    @Column(name = "tracking_number", length = 100)
    private String trackingNumber;

    @Column(name = "alipay_trade_no", length = 64)
    private String alipayTradeNo;

    @Column(name = "expired_at")
    private LocalDateTime expiredAt;

    @Column(name = "refund_reason", length = 500)
    private String refundReason;

    @Column(name = "refund_reject_reason", length = 500)
    private String refundRejectReason;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OrderItem> items;

    @Transient
    private ShipmentResponse shipment;

    @Transient
    private List<OrderCouponUsage> couponUsages;

    @PrePersist
    private void prePersist() { createdAt = LocalDateTime.now(); }
}
