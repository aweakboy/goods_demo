package com.trading.controller;

import com.trading.common.ApiResponse;
import com.trading.dto.ShopRequest;
import com.trading.dto.ShopResponse;
import com.trading.entity.User;
import com.trading.service.ShopService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/seller/shop")
@RequiredArgsConstructor
public class SellerShopController {

    private final ShopService shopService;

    @GetMapping
    public ApiResponse<ShopResponse> getMyShop(@AuthenticationPrincipal User user) {
        return ApiResponse.ok(shopService.getMyShop(user.getId()));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ShopResponse> register(@AuthenticationPrincipal User user,
                                               @Valid @RequestBody ShopRequest req) {
        return ApiResponse.created(shopService.register(user.getId(), req));
    }

    @PutMapping
    public ApiResponse<ShopResponse> update(@AuthenticationPrincipal User user,
                                             @Valid @RequestBody ShopRequest req) {
        return ApiResponse.ok(shopService.update(user.getId(), req));
    }
}
