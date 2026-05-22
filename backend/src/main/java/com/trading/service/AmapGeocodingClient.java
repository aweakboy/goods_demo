package com.trading.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trading.config.MapServiceConfig;
import com.trading.dto.AddressValidationRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AmapGeocodingClient implements MapGeocodingClient {

    private static final String GEOCODE_URL = "https://restapi.amap.com/v3/geocode/geo";

    private final MapServiceConfig config;
    private final ObjectMapper objectMapper;

    @Override
    public List<MapGeocodingResult> geocode(AddressValidationRequest request) {
        if (!config.isEnabled() || isBlank(config.getWebServiceKey())) {
            throw new MapServiceUnavailableException("地址校验暂不可用：地图服务未配置");
        }

        try {
            URI uri = UriComponentsBuilder.fromHttpUrl(GEOCODE_URL)
                    .queryParam("key", config.getWebServiceKey())
                    .queryParam("address", buildFullAddress(request))
                    .queryParam("city", request.getCity())
                    .queryParam("output", "JSON")
                    .build()
                    .encode()
                    .toUri();

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofMillis(config.getConnectTimeoutMs()))
                    .build();
            HttpRequest httpRequest = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofMillis(config.getReadTimeoutMs()))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new MapServiceUnavailableException("地址校验暂不可用：地图服务响应异常");
            }

            return parseResponse(response.body());
        } catch (MapServiceUnavailableException e) {
            throw e;
        } catch (Exception e) {
            throw new MapServiceUnavailableException("地址校验暂不可用，请稍后重试", e);
        }
    }

    private List<MapGeocodingResult> parseResponse(String body) throws Exception {
        JsonNode root = objectMapper.readTree(body);
        if (!"1".equals(root.path("status").asText())) {
            String info = root.path("info").asText("地图服务返回失败");
            throw new MapServiceUnavailableException("地址校验暂不可用：" + info);
        }

        List<MapGeocodingResult> results = new ArrayList<>();
        JsonNode geocodes = root.path("geocodes");
        if (geocodes.isArray()) {
            for (JsonNode item : geocodes) {
                String location = item.path("location").asText("");
                String[] parts = location.split(",");
                if (parts.length != 2 || isBlank(parts[0]) || isBlank(parts[1])) {
                    continue;
                }
                results.add(MapGeocodingResult.builder()
                        .formattedAddress(text(item, "formatted_address"))
                        .province(text(item, "province"))
                        .city(text(item, "city"))
                        .district(text(item, "district"))
                        .adcode(text(item, "adcode"))
                        .level(text(item, "level"))
                        .longitude(new BigDecimal(parts[0].trim()))
                        .latitude(new BigDecimal(parts[1].trim()))
                        .build());
            }
        }
        return results;
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isArray()) {
            return value.size() == 0 ? "" : value.get(0).asText("");
        }
        return value.asText("");
    }

    private String buildFullAddress(AddressValidationRequest request) {
        return trim(request.getProvince()) + trim(request.getCity()) + trim(request.getDistrict()) + trim(request.getDetailAddress());
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
