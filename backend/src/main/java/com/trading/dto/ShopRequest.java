package com.trading.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ShopRequest {
    @NotBlank(message = "店铺名称不能为空")
    @Size(max = 100, message = "店铺名称最多100个字符")
    private String name;

    private String description;
}
