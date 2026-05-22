package com.trading.controller;

import com.trading.common.ApiResponse;
import com.trading.dto.PriceAlertRequest;
import com.trading.dto.PriceAlertResponse;
import com.trading.entity.User;
import com.trading.service.PriceAlertService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/buyer/price-alerts")
@RequiredArgsConstructor
public class BuyerPriceAlertController {

    private final PriceAlertService priceAlertService;

    @GetMapping
    public ApiResponse<Page<PriceAlertResponse>> mine(@AuthenticationPrincipal User user,
                                                      @RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(priceAlertService.listAlerts(user.getId(), page, size));
    }

    @GetMapping("/products/{productId}")
    public ApiResponse<PriceAlertResponse> current(@AuthenticationPrincipal User user,
                                                   @PathVariable Long productId) {
        return ApiResponse.ok(priceAlertService.getAlert(user.getId(), productId));
    }

    @PostMapping("/products/{productId}")
    public ApiResponse<PriceAlertResponse> createOrUpdate(@AuthenticationPrincipal User user,
                                                          @PathVariable Long productId,
                                                          @Valid @RequestBody PriceAlertRequest request) {
        return ApiResponse.created(PriceAlertResponse.from(
                priceAlertService.createOrUpdateAlert(user.getId(), productId, request.getTargetPrice())
        ));
    }

    @DeleteMapping("/products/{productId}")
    public ApiResponse<Void> cancel(@AuthenticationPrincipal User user,
                                    @PathVariable Long productId) {
        priceAlertService.cancelAlert(user.getId(), productId);
        return ApiResponse.ok();
    }
}
