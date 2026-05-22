package com.trading.controller;

import com.trading.common.ApiResponse;
import com.trading.dto.CarrierOptionResponse;
import com.trading.dto.ProductRequest;
import com.trading.dto.ShipRequest;
import com.trading.entity.*;
import com.trading.enums.OrderStatus;
import com.trading.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/seller")
@RequiredArgsConstructor
public class SellerController {

    private final ProductService productService;
    private final OrderService orderService;
    private final FileStorageService fileStorageService;
    private final ShipmentService shipmentService;

    @PostMapping("/products/upload-image")
    public ApiResponse<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            String filename = fileStorageService.store(file);
            return ApiResponse.ok(Map.of("url", "/uploads/" + filename));
        } catch (java.io.IOException e) {
            throw new com.trading.common.BusinessException(500, "文件保存失败");
        }
    }

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

    @GetMapping("/shipping/carriers")
    public ApiResponse<List<CarrierOptionResponse>> carriers() {
        return ApiResponse.ok(shipmentService.getCarrierOptions());
    }

    @PostMapping("/orders/{id}/ship")
    public ApiResponse<Order> ship(@AuthenticationPrincipal User user,
                                    @PathVariable Long id,
                                    @Valid @RequestBody ShipRequest req) {
        return ApiResponse.ok(orderService.ship(id, user.getId(), req));
    }
}
