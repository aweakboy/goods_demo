package com.trading.controller;

import com.trading.common.ApiResponse;
import com.trading.dto.ProductRequest;
import com.trading.entity.*;
import com.trading.enums.OrderStatus;
import com.trading.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/seller")
@RequiredArgsConstructor
public class SellerController {

    private final ProductService productService;
    private final OrderService orderService;

    @GetMapping("/products")
    public ApiResponse<List<Product>> myProducts(@AuthenticationPrincipal User user) {
        return ApiResponse.ok(productService.getSellerProducts(user.getId()));
    }

    @PostMapping("/products")
    public ApiResponse<Product> create(@AuthenticationPrincipal User user,
                                        @Valid @RequestBody ProductRequest req) {
        return ApiResponse.created(productService.createProduct(req, user.getId()));
    }

    @PutMapping("/products/{id}")
    public ApiResponse<Product> update(@AuthenticationPrincipal User user,
                                        @PathVariable Long id,
                                        @Valid @RequestBody ProductRequest req) {
        return ApiResponse.ok(productService.updateProduct(id, req, user.getId()));
    }

    @GetMapping("/orders")
    public ApiResponse<List<Order>> orders(@AuthenticationPrincipal User user,
                                            @RequestParam(required = false) OrderStatus status) {
        return ApiResponse.ok(orderService.getSellerOrders(user.getId(), status));
    }

    @PostMapping("/orders/{id}/ship")
    public ApiResponse<Order> ship(@AuthenticationPrincipal User user,
                                    @PathVariable Long id,
                                    @RequestBody Map<String, String> body) {
        return ApiResponse.ok(orderService.ship(id, user.getId(), body.get("trackingNumber")));
    }
}
