package com.trading.controller;

import com.trading.common.ApiResponse;
import com.trading.dto.MembershipMonthlyBenefitResponse;
import com.trading.dto.MembershipPlanResponse;
import com.trading.dto.MembershipPurchaseResponse;
import com.trading.dto.MembershipStatusResponse;
import com.trading.entity.MembershipPurchase;
import com.trading.entity.User;
import com.trading.service.MembershipService;
import com.trading.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/buyer/membership")
@RequiredArgsConstructor
public class BuyerMembershipController {

    private final MembershipService membershipService;
    private final PaymentService paymentService;

    @GetMapping("/plans")
    public ApiResponse<List<MembershipPlanResponse>> plans() {
        return ApiResponse.ok(membershipService.listActivePlans().stream()
                .map(MembershipPlanResponse::from)
                .toList());
    }

    @GetMapping("/status")
    public ApiResponse<MembershipStatusResponse> status(@AuthenticationPrincipal User user) {
        MembershipService.MembershipStatus status = membershipService.status(user.getId());
        if (status.membership() == null) {
            return ApiResponse.ok(MembershipStatusResponse.nonMember());
        }
        return ApiResponse.ok(MembershipStatusResponse.from(
                status.membership(),
                status.displayStatus(),
                status.currentMonthBenefitClaimed()
        ));
    }

    @GetMapping("/purchases")
    public ApiResponse<List<MembershipPurchaseResponse>> purchases(@AuthenticationPrincipal User user) {
        return ApiResponse.ok(membershipService.listPurchases(user.getId()).stream()
                .map(MembershipPurchaseResponse::from)
                .toList());
    }

    @PostMapping(value = "/plans/{planId}/purchase", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> purchase(@AuthenticationPrincipal User user, @PathVariable Long planId) {
        MembershipPurchase purchase = membershipService.createPurchase(user.getId(), planId);
        String html = paymentService.createPayForm(
                purchase.getOutTradeNo(),
                purchase.getAmount(),
                "会员套餐" + purchase.getPlanId()
        );
        return ResponseEntity.ok(html);
    }

    @PostMapping(value = "/purchases/{purchaseId}/pay", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> payPurchase(@AuthenticationPrincipal User user, @PathVariable Long purchaseId) {
        MembershipPurchase purchase = membershipService.preparePendingPurchasePayment(user.getId(), purchaseId);
        String html = paymentService.createPayForm(
                purchase.getOutTradeNo(),
                purchase.getAmount(),
                "会员套餐" + purchase.getPlanId()
        );
        return ResponseEntity.ok(html);
    }

    @PostMapping("/benefits/monthly/claim")
    public ApiResponse<MembershipMonthlyBenefitResponse> claimMonthlyBenefit(@AuthenticationPrincipal User user) {
        return ApiResponse.created(MembershipMonthlyBenefitResponse.from(
                membershipService.claimMonthlyBenefit(user.getId())
        ));
    }
}
