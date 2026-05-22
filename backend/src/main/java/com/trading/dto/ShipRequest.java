package com.trading.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ShipRequest {
    @Size(max = 50, message = "物流公司编码最多50个字符")
    private String carrierCode;

    @Size(max = 100, message = "物流公司名称最多100个字符")
    private String carrierName;

    @Size(max = 100, message = "物流单号最多100个字符")
    private String trackingNumber;
}
