package com.trading.dto;

import com.trading.enums.CouponAudience;
import com.trading.enums.CouponStatus;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CouponRequest {
    @NotBlank(message = "优惠券名称不能为空")
    @Size(max = 100, message = "优惠券名称最多100个字符")
    private String name;

    @Size(max = 500, message = "优惠券说明最多500个字符")
    private String description;

    @NotNull(message = "满减门槛不能为空")
    @DecimalMin(value = "0.00", message = "满减门槛不能小于0")
    private BigDecimal thresholdAmount;

    @NotNull(message = "抵扣金额不能为空")
    @DecimalMin(value = "0.01", message = "抵扣金额必须大于0")
    private BigDecimal discountAmount;

    @NotNull(message = "发放总量不能为空")
    @Min(value = 1, message = "发放总量必须大于0")
    private Integer totalQuantity;

    @NotNull(message = "每人领取上限不能为空")
    @Min(value = 1, message = "每人领取上限必须大于0")
    private Integer perUserLimit;

    @NotNull(message = "有效期开始时间不能为空")
    private LocalDateTime validFrom;

    @NotNull(message = "有效期结束时间不能为空")
    private LocalDateTime validTo;

    private CouponStatus status;

    private CouponAudience audience;

    private Boolean stackable;
}
