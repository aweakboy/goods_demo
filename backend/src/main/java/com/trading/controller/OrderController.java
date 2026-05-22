package com.trading.controller;

import com.trading.common.ApiResponse;
import com.trading.dto.OrderRequest;
import com.trading.dto.ReasonRequest;
import com.trading.entity.*;
import com.trading.enums.OrderStatus;
import com.trading.service.OrderService;
import com.trading.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final PaymentService paymentService;

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

    @PostMapping(value = "/{id}/pay", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> pay(@AuthenticationPrincipal User user, @PathVariable Long id) {
        Order order = orderService.validatePayable(id, user.getId());
        String html = paymentService.createPayForm(order.getId(), order.getTotalAmount());
        return ResponseEntity.ok(html);
    }

    @PostMapping("/{id}/payment/reconcile")
    public ApiResponse<Order> reconcilePayment(@AuthenticationPrincipal User user, @PathVariable Long id) {
        return ApiResponse.ok(paymentService.reconcileOrderPayment(id, user.getId()));
    }

    @PostMapping("/{id}/confirm")
    public ApiResponse<Order> confirm(@AuthenticationPrincipal User user, @PathVariable Long id) {
        return ApiResponse.ok(orderService.confirm(id, user.getId()));
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<Order> cancel(@AuthenticationPrincipal User user, @PathVariable Long id) {
        return ApiResponse.ok(orderService.cancel(id, user.getId()));
    }

    @PostMapping("/{id}/refund-request")
    public ApiResponse<Order> refundRequest(@AuthenticationPrincipal User user,
                                             @PathVariable Long id,
                                             @RequestBody ReasonRequest req) {
        return ApiResponse.ok(orderService.requestRefund(id, user.getId(), req.getReason()));
    }
}
