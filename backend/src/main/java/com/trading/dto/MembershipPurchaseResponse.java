package com.trading.dto;

import com.trading.entity.MembershipPurchase;
import com.trading.enums.MembershipPurchaseStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class MembershipPurchaseResponse {
    private Long id;
    private Long buyerId;
    private Long planId;
    private String planName;
    private BigDecimal amount;
    private MembershipPurchaseStatus status;
    private String outTradeNo;
    private String alipayTradeNo;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;

    public static MembershipPurchaseResponse from(MembershipPurchase purchase) {
        return MembershipPurchaseResponse.builder()
                .id(purchase.getId())
                .buyerId(purchase.getBuyerId())
                .planId(purchase.getPlanId())
                .planName(purchase.getPlan() != null ? purchase.getPlan().getName() : null)
                .amount(purchase.getAmount())
                .status(purchase.getStatus())
                .outTradeNo(purchase.getOutTradeNo())
                .alipayTradeNo(purchase.getAlipayTradeNo())
                .paidAt(purchase.getPaidAt())
                .createdAt(purchase.getCreatedAt())
                .build();
    }
}
