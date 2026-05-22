package com.trading.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "map")
public class MapServiceConfig {
    private String provider = "amap";
    private boolean enabled = true;
    private String webServiceKey;
    private int connectTimeoutMs = 3000;
    private int readTimeoutMs = 5000;
}
