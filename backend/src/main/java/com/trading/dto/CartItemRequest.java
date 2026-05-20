package com.trading.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CartItemRequest {
    @NotNull(message = "商品ID不能为空")
    private Long productId;

    @NotNull @Min(value = 1, message = "数量至少为1")
    private Integer quantity;
}
