package com.trading.controller;

import com.trading.common.ApiResponse;
import com.trading.dto.ShopResponse;
import com.trading.dto.ShopStorefrontResponse;
import com.trading.service.ShopService;
import lombok.RequiredArgsConstructor;
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
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(shopService.getStorefront(id, page, size));
    }

    @GetMapping
    public ApiResponse<List<ShopResponse>> search(@RequestParam(required = false) String name) {
        return ApiResponse.ok(shopService.searchShops(name));
    }
}
