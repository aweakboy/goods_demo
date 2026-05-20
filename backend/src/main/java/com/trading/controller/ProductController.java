package com.trading.controller;

import com.trading.common.ApiResponse;
import com.trading.dto.ProductWithShopResponse;
import com.trading.entity.*;
import com.trading.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping("/products")
    public ApiResponse<Page<ProductWithShopResponse>> list(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(productService.searchProducts(categoryId, keyword, page, size));
    }

    @GetMapping("/products/{id}")
    public ApiResponse<ProductWithShopResponse> detail(@PathVariable Long id) {
        return ApiResponse.ok(productService.getProduct(id));
    }

    @GetMapping("/categories")
    public ApiResponse<List<Category>> categories() {
        return ApiResponse.ok(productService.getActiveCategories());
    }
}
