package com.trading.dto;

import com.trading.entity.MembershipMonthlyBenefit;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class MembershipMonthlyBenefitResponse {
    private Long id;
    private Long planId;
    private String benefitMonth;
    private Long couponId;
    private Long buyerCouponId;
    private LocalDateTime claimedAt;

    public static MembershipMonthlyBenefitResponse from(MembershipMonthlyBenefit benefit) {
        return MembershipMonthlyBenefitResponse.builder()
                .id(benefit.getId())
                .planId(benefit.getPlanId())
                .benefitMonth(benefit.getBenefitMonth())
                .couponId(benefit.getCouponId())
                .buyerCouponId(benefit.getBuyerCouponId())
                .claimedAt(benefit.getClaimedAt())
                .build();
    }
}
