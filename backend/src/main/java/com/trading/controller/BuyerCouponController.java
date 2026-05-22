package com.trading.controller;

import com.trading.common.ApiResponse;
import com.trading.dto.BuyerCouponResponse;
import com.trading.dto.CouponResponse;
import com.trading.entity.User;
import com.trading.enums.BuyerCouponStatus;
import com.trading.service.BuyerCouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/buyer/coupons")
@RequiredArgsConstructor
public class BuyerCouponController {

    private final BuyerCouponService buyerCouponService;

    @GetMapping("/claimable")
    public ApiResponse<List<CouponResponse>> claimable(@AuthenticationPrincipal User user) {
        return ApiResponse.ok(buyerCouponService.listClaimableCoupons().stream()
                .map(coupon -> CouponResponse.from(
                        coupon,
                        buyerCouponService.countClaimedByBuyer(user.getId(), coupon.getId()) >= coupon.getPerUserLimit()))
                .toList());
    }

    @PostMapping("/{couponId}/claim")
    public ApiResponse<BuyerCouponResponse> claim(@AuthenticationPrincipal User user,
                                                  @PathVariable Long couponId) {
        var buyerCoupon = buyerCouponService.claim(user.getId(), couponId);
        return ApiResponse.created(BuyerCouponResponse.from(
                buyerCoupon,
                buyerCouponService.displayStatus(buyerCoupon)
        ));
    }

    @GetMapping("/mine")
    public ApiResponse<List<BuyerCouponResponse>> mine(@AuthenticationPrincipal User user,
                                                       @RequestParam(required = false) BuyerCouponStatus status) {
        return ApiResponse.ok(buyerCouponService.listMine(user.getId(), status).stream()
                .map(buyerCoupon -> BuyerCouponResponse.from(
                        buyerCoupon,
                        buyerCouponService.displayStatus(buyerCoupon)))
                .toList());
    }

    @GetMapping("/usable")
    public ApiResponse<List<BuyerCouponResponse>> usable(@AuthenticationPrincipal User user) {
        return ApiResponse.ok(buyerCouponService.listUsable(user.getId()).stream()
                .map(buyerCoupon -> BuyerCouponResponse.from(
                        buyerCoupon,
                        buyerCouponService.displayStatus(buyerCoupon)))
                .toList());
    }
}
