package com.trading.controller;

import com.trading.common.ApiResponse;
import com.trading.dto.ShopFavoriteResponse;
import com.trading.dto.ShopFavoriteStateResponse;
import com.trading.entity.User;
import com.trading.service.ShopFavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/buyer/shop-favorites")
@RequiredArgsConstructor
public class BuyerShopFavoriteController {

    private final ShopFavoriteService shopFavoriteService;

    @GetMapping
    public ApiResponse<Page<ShopFavoriteResponse>> mine(@AuthenticationPrincipal User user,
                                                        @RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(shopFavoriteService.listFavorites(user.getId(), page, size));
    }

    @PostMapping("/{shopId}")
    public ApiResponse<ShopFavoriteStateResponse> favorite(@AuthenticationPrincipal User user,
                                                           @PathVariable Long shopId) {
        return ApiResponse.created(shopFavoriteService.favoriteShop(user.getId(), shopId));
    }

    @DeleteMapping("/{shopId}")
    public ApiResponse<ShopFavoriteStateResponse> unfavorite(@AuthenticationPrincipal User user,
                                                             @PathVariable Long shopId) {
        return ApiResponse.ok(shopFavoriteService.unfavoriteShop(user.getId(), shopId));
    }
}
