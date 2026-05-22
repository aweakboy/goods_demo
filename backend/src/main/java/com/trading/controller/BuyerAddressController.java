package com.trading.controller;

import com.trading.common.ApiResponse;
import com.trading.dto.BuyerAddressRequest;
import com.trading.dto.BuyerAddressResponse;
import com.trading.entity.User;
import com.trading.service.BuyerAddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/buyer/addresses")
@RequiredArgsConstructor
public class BuyerAddressController {

    private final BuyerAddressService buyerAddressService;

    @GetMapping
    public ApiResponse<List<BuyerAddressResponse>> list(@AuthenticationPrincipal User user) {
        return ApiResponse.ok(buyerAddressService.list(user.getId()).stream()
                .map(BuyerAddressResponse::from)
                .toList());
    }

    @PostMapping
    public ApiResponse<BuyerAddressResponse> create(@AuthenticationPrincipal User user,
                                                    @Valid @RequestBody BuyerAddressRequest request) {
        return ApiResponse.created(BuyerAddressResponse.from(buyerAddressService.create(user.getId(), request)));
    }

    @PutMapping("/{id}")
    public ApiResponse<BuyerAddressResponse> update(@AuthenticationPrincipal User user,
                                                    @PathVariable Long id,
                                                    @Valid @RequestBody BuyerAddressRequest request) {
        return ApiResponse.ok(BuyerAddressResponse.from(buyerAddressService.update(user.getId(), id, request)));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal User user, @PathVariable Long id) {
        buyerAddressService.delete(user.getId(), id);
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/default")
    public ApiResponse<BuyerAddressResponse> setDefault(@AuthenticationPrincipal User user,
                                                        @PathVariable Long id) {
        return ApiResponse.ok(BuyerAddressResponse.from(buyerAddressService.setDefault(user.getId(), id)));
    }
}
