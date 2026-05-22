package com.trading.controller;

import com.trading.common.ApiResponse;
import com.trading.dto.*;
import com.trading.entity.OperationLog;
import com.trading.entity.Order;
import com.trading.entity.User;
import com.trading.service.AdminService;
import com.trading.service.OperationLogService;
import com.trading.service.OrderService;
import com.trading.service.ShipmentSimulationService;
import com.trading.service.ShipmentService;
import com.trading.service.ShopService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final ShopService shopService;
    private final OrderService orderService;
    private final OperationLogService operationLogService;
    private final ShipmentSimulationService shipmentSimulationService;
    private final ShipmentService shipmentService;

    @GetMapping("/overview")
    public ApiResponse<AdminOverviewResponse> overview() {
        return ApiResponse.ok(adminService.getOverview());
    }

    @GetMapping("/users")
    public ApiResponse<Page<AdminUserResponse>> users(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(adminService.getUsers(role, status, page, size));
    }

    @PutMapping("/users/{id}/status")
    public ApiResponse<AdminUserResponse> updateUserStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStatusRequest req,
            @AuthenticationPrincipal User currentAdmin) {
        return ApiResponse.ok(adminService.updateUserStatus(id, req.getStatus(), currentAdmin.getId()));
    }

    @GetMapping("/products")
    public ApiResponse<Page<AdminProductResponse>> products(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String sellerName,
            @RequestParam(required = false) String shopName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(adminService.getProducts(status, sellerName, shopName, page, size));
    }

    @PutMapping("/products/{id}/status")
    public ApiResponse<Void> updateProductStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStatusRequest req) {
        adminService.updateProductStatus(id, req.getStatus());
        return ApiResponse.ok();
    }

    @GetMapping("/categories")
    public ApiResponse<List<AdminCategoryResponse>> categories() {
        return ApiResponse.ok(adminService.getCategories());
    }

    @PostMapping("/categories")
    public ApiResponse<AdminCategoryResponse> createCategory(@Valid @RequestBody CategoryRequest req) {
        return ApiResponse.ok(adminService.createCategory(req.getName()));
    }

    @PutMapping("/categories/{id}")
    public ApiResponse<AdminCategoryResponse> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequest req) {
        return ApiResponse.ok(adminService.updateCategory(id, req.getName()));
    }

    @PutMapping("/categories/{id}/status")
    public ApiResponse<AdminCategoryResponse> updateCategoryStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStatusRequest req) {
        return ApiResponse.ok(adminService.updateCategoryStatus(id, req.getStatus()));
    }

    @GetMapping("/shops")
    public ApiResponse<Page<AdminShopResponse>> shops(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(shopService.getShopsForAdmin(status, page, size));
    }

    @PutMapping("/shops/{id}/status")
    public ApiResponse<AdminShopResponse> updateShopStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStatusRequest req) {
        return ApiResponse.ok(shopService.updateShopStatus(id, req.getStatus()));
    }

    @GetMapping("/orders")
    public ApiResponse<Page<AdminOrderResponse>> orders(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(adminService.getOrders(status, page, size));
    }

    @GetMapping("/orders/{id}")
    public ApiResponse<AdminOrderResponse> orderDetail(@PathVariable Long id) {
        return ApiResponse.ok(adminService.getOrderDetail(id));
    }

    @PostMapping("/shipments/{id}/simulate/advance")
    public ApiResponse<ShipmentResponse> advanceShipment(@PathVariable Long id) {
        return ApiResponse.ok(shipmentSimulationService.advance(id));
    }

    @PostMapping("/shipments/{id}/simulate/exception")
    public ApiResponse<ShipmentResponse> markShipmentException(@PathVariable Long id,
                                                               @RequestBody ReasonRequest req) {
        return ApiResponse.ok(shipmentSimulationService.markException(id, req.getReason()));
    }

    @PostMapping("/shipments/{id}/route/refresh")
    public ApiResponse<ShipmentResponse> refreshShipmentRoute(@PathVariable Long id) {
        return ApiResponse.ok(shipmentService.refreshRoute(id));
    }

    @GetMapping("/orders/refund-requests")
    public ApiResponse<List<Order>> refundRequests() {
        return ApiResponse.ok(orderService.getRefundRequests());
    }

    @PostMapping("/orders/{id}/refund-approve")
    public ApiResponse<Order> refundApprove(@PathVariable Long id) {
        return ApiResponse.ok(orderService.approveRefund(id));
    }

    @PostMapping("/orders/{id}/refund-reject")
    public ApiResponse<Order> refundReject(@PathVariable Long id,
                                            @RequestBody ReasonRequest req) {
        return ApiResponse.ok(orderService.rejectRefund(id, req.getReason()));
    }

    @GetMapping("/logs")
    public ApiResponse<Page<OperationLog>> getLogs(
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(operationLogService.getLogs(module, username, startTime, endTime, page, size));
    }
}
