package com.trading.dto;

import com.trading.entity.Coupon;
import com.trading.enums.CouponAudience;
import com.trading.enums.CouponStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class CouponResponse {
    private Long id;
    private String name;
    private String description;
    private BigDecimal thresholdAmount;
    private BigDecimal discountAmount;
    private Integer totalQuantity;
    private Integer claimedQuantity;
    private Integer remainingQuantity;
    private Integer perUserLimit;
    private LocalDateTime validFrom;
    private LocalDateTime validTo;
    private CouponStatus status;
    private CouponAudience audience;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean claimLimitReached;

    public static CouponResponse from(Coupon coupon) {
        return from(coupon, null);
    }

    public static CouponResponse from(Coupon coupon, Boolean claimLimitReached) {
        int claimed = coupon.getClaimedQuantity() == null ? 0 : coupon.getClaimedQuantity();
        int total = coupon.getTotalQuantity() == null ? 0 : coupon.getTotalQuantity();
        return CouponResponse.builder()
                .id(coupon.getId())
                .name(coupon.getName())
                .description(coupon.getDescription())
                .thresholdAmount(coupon.getThresholdAmount())
                .discountAmount(coupon.getDiscountAmount())
                .totalQuantity(coupon.getTotalQuantity())
                .claimedQuantity(claimed)
                .remainingQuantity(Math.max(0, total - claimed))
                .perUserLimit(coupon.getPerUserLimit())
                .validFrom(coupon.getValidFrom())
                .validTo(coupon.getValidTo())
                .status(coupon.getStatus())
                .audience(coupon.getAudience())
                .createdBy(coupon.getCreatedBy())
                .createdAt(coupon.getCreatedAt())
                .updatedAt(coupon.getUpdatedAt())
                .claimLimitReached(claimLimitReached)
                .build();
    }
}
