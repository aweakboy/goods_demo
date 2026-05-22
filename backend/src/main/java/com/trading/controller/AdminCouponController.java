package com.trading.controller;

import com.trading.common.ApiResponse;
import com.trading.dto.CouponRequest;
import com.trading.dto.CouponResponse;
import com.trading.entity.User;
import com.trading.enums.CouponStatus;
import com.trading.service.CouponAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/coupons")
@RequiredArgsConstructor
public class AdminCouponController {

    private final CouponAdminService couponAdminService;

    @GetMapping
    public ApiResponse<Page<CouponResponse>> list(
            @RequestParam(required = false) CouponStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(couponAdminService.list(status, page, size).map(CouponResponse::from));
    }

    @PostMapping
    public ApiResponse<CouponResponse> create(@AuthenticationPrincipal User user,
                                              @Valid @RequestBody CouponRequest request) {
        return ApiResponse.created(CouponResponse.from(couponAdminService.create(user.getId(), request)));
    }

    @PutMapping("/{id}")
    public ApiResponse<CouponResponse> update(@PathVariable Long id,
                                              @Valid @RequestBody CouponRequest request) {
        return ApiResponse.ok(CouponResponse.from(couponAdminService.update(id, request)));
    }

    @PostMapping("/{id}/activate")
    public ApiResponse<CouponResponse> activate(@PathVariable Long id) {
        return ApiResponse.ok(CouponResponse.from(couponAdminService.activate(id)));
    }

    @PostMapping("/{id}/deactivate")
    public ApiResponse<CouponResponse> deactivate(@PathVariable Long id) {
        return ApiResponse.ok(CouponResponse.from(couponAdminService.deactivate(id)));
    }
}
