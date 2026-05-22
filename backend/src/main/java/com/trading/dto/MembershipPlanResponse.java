package com.trading.dto;

import com.trading.entity.Coupon;
import com.trading.entity.MembershipPlan;
import com.trading.enums.MembershipPlanStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class MembershipPlanResponse {
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer durationMonths;
    private BigDecimal discountRate;
    private Long monthlyCouponId;
    private String monthlyCouponName;
    private BigDecimal monthlyCouponThresholdAmount;
    private BigDecimal monthlyCouponDiscountAmount;
    private MembershipPlanStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static MembershipPlanResponse from(MembershipPlan plan) {
        Coupon coupon = plan.getMonthlyCoupon();
        return MembershipPlanResponse.builder()
                .id(plan.getId())
                .name(plan.getName())
                .description(plan.getDescription())
                .price(plan.getPrice())
                .durationMonths(plan.getDurationMonths())
                .discountRate(plan.getDiscountRate())
                .monthlyCouponId(plan.getMonthlyCouponId())
                .monthlyCouponName(coupon != null ? coupon.getName() : null)
                .monthlyCouponThresholdAmount(coupon != null ? coupon.getThresholdAmount() : null)
                .monthlyCouponDiscountAmount(coupon != null ? coupon.getDiscountAmount() : null)
                .status(plan.getStatus())
                .createdAt(plan.getCreatedAt())
                .updatedAt(plan.getUpdatedAt())
                .build();
    }
}
