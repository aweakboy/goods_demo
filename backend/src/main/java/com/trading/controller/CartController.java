package com.trading.controller;

import com.trading.common.ApiResponse;
import com.trading.dto.CartItemRequest;
import com.trading.entity.*;
import com.trading.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping("/items")
    public ApiResponse<List<CartItem>> getCart(@AuthenticationPrincipal User user) {
        return ApiResponse.ok(cartService.getCart(user.getId()));
    }

    @PostMapping("/items")
    public ApiResponse<CartItem> addItem(@AuthenticationPrincipal User user,
                                         @Valid @RequestBody CartItemRequest req) {
        return ApiResponse.ok(cartService.addItem(user.getId(), req));
    }

    @PutMapping("/items/{id}")
    public ApiResponse<CartItem> updateItem(@AuthenticationPrincipal User user,
                                             @PathVariable Long id,
                                             @RequestBody Map<String, Integer> body) {
        return ApiResponse.ok(cartService.updateItem(user.getId(), id, body.get("quantity")));
    }

    @DeleteMapping("/items/{id}")
    public ApiResponse<Void> removeItem(@AuthenticationPrincipal User user, @PathVariable Long id) {
        cartService.removeItem(user.getId(), id);
        return ApiResponse.ok();
    }

    @DeleteMapping
    public ApiResponse<Void> clearCart(@AuthenticationPrincipal User user) {
        cartService.clearCart(user.getId());
        return ApiResponse.ok();
    }
}
