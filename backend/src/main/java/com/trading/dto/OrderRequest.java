package com.trading.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class OrderRequest {
    @NotBlank(message = "收货地址不能为空")
    private String address;

    private List<Long> cartItemIds;
}
