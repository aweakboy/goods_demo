package com.trading.controller;

import com.trading.common.ApiResponse;
import com.trading.dto.ShopResponse;
import com.trading.dto.ShopStorefrontResponse;
import com.trading.entity.User;
import com.trading.enums.Role;
import com.trading.service.ShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/shops")
@RequiredArgsConstructor
public class ShopController {

    private final ShopService shopService;

    @GetMapping("/{id}")
    public ApiResponse<ShopStorefrontResponse> storefront(
            @PathVariable Long id,
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long buyerId = user != null && user.getRole() == Role.BUYER ? user.getId() : null;
        return ApiResponse.ok(shopService.getStorefront(id, page, size, buyerId));
    }

    @GetMapping
    public ApiResponse<List<ShopResponse>> search(@RequestParam(required = false) String name,
                                                  @AuthenticationPrincipal User user) {
        Long buyerId = user != null && user.getRole() == Role.BUYER ? user.getId() : null;
        return ApiResponse.ok(shopService.searchShops(name, buyerId));
    }
}
