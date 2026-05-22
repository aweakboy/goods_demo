package com.trading.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class BuyerAddressRequest {
    @NotBlank(message = "收货人不能为空")
    @Size(max = 50, message = "收货人姓名最多50个字符")
    private String receiverName;

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
    private String receiverPhone;

    @NotBlank(message = "省份不能为空")
    @Size(max = 50, message = "省份最多50个字符")
    private String province;

    @NotBlank(message = "城市不能为空")
    @Size(max = 50, message = "城市最多50个字符")
    private String city;

    @NotBlank(message = "区县不能为空")
    @Size(max = 50, message = "区县最多50个字符")
    private String district;

    @NotBlank(message = "详细地址不能为空")
    @Size(max = 300, message = "详细地址最多300个字符")
    private String detailAddress;

    private Boolean defaultAddress;
}
