package com.trading.controller;

import com.trading.common.ApiResponse;
import com.trading.dto.OrderRequest;
import com.trading.entity.*;
import com.trading.enums.OrderStatus;
import com.trading.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ApiResponse<Order> create(@AuthenticationPrincipal User user,
                                     @Valid @RequestBody OrderRequest req) {
        return ApiResponse.created(orderService.createOrder(user.getId(), req));
    }

    @GetMapping
    public ApiResponse<List<Order>> list(@AuthenticationPrincipal User user,
                                          @RequestParam(required = false) OrderStatus status) {
        return ApiResponse.ok(orderService.getBuyerOrders(user.getId(), status));
    }

    @GetMapping("/{id}")
    public ApiResponse<Order> detail(@AuthenticationPrincipal User user, @PathVariable Long id) {
        return ApiResponse.ok(orderService.getOrderDetail(id, user.getId()));
    }

    @PostMapping("/{id}/pay")
    public ApiResponse<Order> pay(@AuthenticationPrincipal User user, @PathVariable Long id) {
        return ApiResponse.ok(orderService.pay(id, user.getId()));
    }

    @PostMapping("/{id}/confirm")
    public ApiResponse<Order> confirm(@AuthenticationPrincipal User user, @PathVariable Long id) {
        return ApiResponse.ok(orderService.confirm(id, user.getId()));
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<Order> cancel(@AuthenticationPrincipal User user, @PathVariable Long id) {
        return ApiResponse.ok(orderService.cancel(id, user.getId()));
    }
}
