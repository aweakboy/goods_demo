package com.trading.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class OrderRequest {
    @Min(value = 1, message = "地址ID不正确")
    private Long addressId;

    @Min(value = 1, message = "优惠券ID不正确")
    private Long buyerCouponId;

    @Size(max = 2, message = "最多只能选择2张优惠券")
    private List<@Min(value = 1, message = "优惠券ID不正确") Long> buyerCouponIds;

    @Size(max = 500, message = "收货地址最多500个字符")
    private String address;

    @Size(max = 50, message = "收货人姓名最多50个字符")
    private String receiverName;

    @Pattern(regexp = "^$|^1\\d{10}$", message = "手机号格式不正确")
    private String receiverPhone;

    @Size(max = 50, message = "省份最多50个字符")
    private String receiverProvince;

    @Size(max = 50, message = "城市最多50个字符")
    private String receiverCity;

    @Size(max = 50, message = "区县最多50个字符")
    private String receiverDistrict;

    @Size(max = 300, message = "详细地址最多300个字符")
    private String receiverDetailAddress;

    @DecimalMin(value = "-180.0", message = "经度不能小于-180")
    @DecimalMax(value = "180.0", message = "经度不能大于180")
    private BigDecimal receiverLongitude;

    @DecimalMin(value = "-90.0", message = "纬度不能小于-90")
    @DecimalMax(value = "90.0", message = "纬度不能大于90")
    private BigDecimal receiverLatitude;

    private List<Long> cartItemIds;
}
