package com.trading.dto;

import com.trading.entity.BuyerMembership;
import com.trading.entity.MembershipPlan;
import com.trading.enums.BuyerMembershipStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class MembershipStatusResponse {
    private Boolean member;
    private BuyerMembershipStatus status;
    private Long planId;
    private String planName;
    private BigDecimal discountRate;
    private LocalDateTime startedAt;
    private LocalDateTime expiresAt;
    private Boolean currentMonthBenefitClaimed;
    private Long monthlyCouponId;
    private String monthlyCouponName;

    public static MembershipStatusResponse nonMember() {
        return MembershipStatusResponse.builder()
                .member(false)
                .currentMonthBenefitClaimed(false)
                .build();
    }

    public static MembershipStatusResponse from(BuyerMembership membership,
                                                BuyerMembershipStatus displayStatus,
                                                boolean benefitClaimed) {
        MembershipPlan plan = membership.getPlan();
        return MembershipStatusResponse.builder()
                .member(displayStatus == BuyerMembershipStatus.ACTIVE)
                .status(displayStatus)
                .planId(membership.getPlanId())
                .planName(plan != null ? plan.getName() : null)
                .discountRate(plan != null ? plan.getDiscountRate() : null)
                .startedAt(membership.getStartedAt())
                .expiresAt(membership.getExpiresAt())
                .currentMonthBenefitClaimed(benefitClaimed)
                .monthlyCouponId(plan != null ? plan.getMonthlyCouponId() : null)
                .monthlyCouponName(plan != null && plan.getMonthlyCoupon() != null ? plan.getMonthlyCoupon().getName() : null)
                .build();
    }
}
