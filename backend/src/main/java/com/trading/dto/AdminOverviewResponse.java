package com.trading.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class AdminOverviewResponse {
    private long totalUsers;
    private long activeProducts;
    private long todayOrders;
    private BigDecimal totalRevenue;
}
