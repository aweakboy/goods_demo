package com.trading.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PriceAlertRequest {
    @NotNull(message = "目标价不能为空")
    @DecimalMin(value = "0.01", message = "目标价必须大于0")
    private BigDecimal targetPrice;
}
