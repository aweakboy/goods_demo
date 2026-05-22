package com.trading.dto;

import com.trading.entity.BuyerCoupon;
import com.trading.entity.Coupon;
import com.trading.enums.BuyerCouponStatus;
import com.trading.enums.CouponAudience;
import com.trading.enums.CouponStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class BuyerCouponResponse {
    private Long id;
    private Long couponId;
    private String couponName;
    private String description;
    private BigDecimal thresholdAmount;
    private BigDecimal discountAmount;
    private CouponAudience audience;
    private Boolean stackable;
    private LocalDateTime validFrom;
    private LocalDateTime validTo;
    private CouponStatus couponStatus;
    private BuyerCouponStatus status;
    private LocalDateTime claimedAt;
    private LocalDateTime usedAt;
    private Long usedOrderId;

    public static BuyerCouponResponse from(BuyerCoupon buyerCoupon, BuyerCouponStatus displayStatus) {
        Coupon coupon = buyerCoupon.getCoupon();
        return BuyerCouponResponse.builder()
                .id(buyerCoupon.getId())
                .couponId(buyerCoupon.getCouponId())
                .couponName(coupon != null ? coupon.getName() : null)
                .description(coupon != null ? coupon.getDescription() : null)
                .thresholdAmount(coupon != null ? coupon.getThresholdAmount() : null)
                .discountAmount(coupon != null ? coupon.getDiscountAmount() : null)
                .audience(coupon != null ? coupon.getAudience() : null)
                .stackable(coupon != null && Boolean.TRUE.equals(coupon.getStackable()))
                .validFrom(coupon != null ? coupon.getValidFrom() : null)
                .validTo(coupon != null ? coupon.getValidTo() : null)
                .couponStatus(coupon != null ? coupon.getStatus() : null)
                .status(displayStatus)
                .claimedAt(buyerCoupon.getClaimedAt())
                .usedAt(buyerCoupon.getUsedAt())
                .usedOrderId(buyerCoupon.getUsedOrderId())
                .build();
    }
}
