package com.trading.dto;

import com.trading.enums.MembershipPlanStatus;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class MembershipPlanRequest {
    @NotBlank(message = "会员套餐名称不能为空")
    @Size(max = 100, message = "会员套餐名称最多100个字符")
    private String name;

    @Size(max = 500, message = "会员套餐说明最多500个字符")
    private String description;

    @NotNull(message = "会员价格不能为空")
    @DecimalMin(value = "0.01", message = "会员价格必须大于0")
    private BigDecimal price;

    @NotNull(message = "会员有效期不能为空")
    @Min(value = 1, message = "会员有效期至少1个月")
    private Integer durationMonths;

    @NotNull(message = "会员折扣率不能为空")
    @DecimalMin(value = "0.0001", message = "会员折扣率必须大于0")
    @DecimalMax(value = "1.0000", message = "会员折扣率不能大于1")
    private BigDecimal discountRate;

    private Long monthlyCouponId;

    private MembershipPlanStatus status;
}
