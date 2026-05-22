package com.trading.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "shipping.route-planning")
public class RoutePlanningConfig {
    private boolean enabled = true;
    private String provider = "amap";
    private int connectTimeoutMs = 3000;
    private int readTimeoutMs = 5000;
    private String strategy = "0";
    private int minRefreshIntervalMinutes = 10;
}
