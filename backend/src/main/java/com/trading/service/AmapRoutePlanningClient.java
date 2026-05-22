package com.trading.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trading.config.MapServiceConfig;
import com.trading.config.RoutePlanningConfig;
import com.trading.dto.RoutePlanningPoint;
import com.trading.dto.RoutePlanningRequest;
import com.trading.dto.RoutePlanningResult;
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
public class AmapRoutePlanningClient implements RoutePlanningClient {

    private static final String DRIVING_URL = "https://restapi.amap.com/v3/direction/driving";
    private static final String PROVIDER = "AMAP";

    private final MapServiceConfig mapConfig;
    private final RoutePlanningConfig routeConfig;
    private final ObjectMapper objectMapper;

    @Override
    public RoutePlanningResult plan(RoutePlanningRequest request) {
        if (!mapConfig.isEnabled() || !routeConfig.isEnabled() || isBlank(mapConfig.getWebServiceKey())) {
            throw new RoutePlanningException("路径规划暂不可用：地图服务未配置");
        }
        try {
            URI uri = UriComponentsBuilder.fromHttpUrl(DRIVING_URL)
                    .queryParam("key", mapConfig.getWebServiceKey())
                    .queryParam("origin", coordinate(request.getOriginLongitude(), request.getOriginLatitude()))
                    .queryParam("destination", coordinate(request.getDestinationLongitude(), request.getDestinationLatitude()))
                    .queryParam("strategy", routeConfig.getStrategy())
                    .queryParam("extensions", "base")
                    .queryParam("output", "JSON")
                    .build()
                    .encode()
                    .toUri();
            HttpResponse<String> response = send(uri);
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new RoutePlanningException("路径规划暂不可用：地图服务响应异常");
            }
            return parseResponse(response.body());
        } catch (RoutePlanningException e) {
            throw e;
        } catch (Exception e) {
            throw new RoutePlanningException("路径规划暂不可用，请稍后重试", e);
        }
    }

    protected HttpResponse<String> send(URI uri) throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(routeConfig.getConnectTimeoutMs()))
                .build();
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofMillis(routeConfig.getReadTimeoutMs()))
                .GET()
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    RoutePlanningResult parseResponse(String body) throws Exception {
        JsonNode root = objectMapper.readTree(body);
        if (!"1".equals(root.path("status").asText())) {
            String info = root.path("info").asText("地图服务返回失败");
            throw new RoutePlanningException("路径规划暂不可用：" + info);
        }

        JsonNode paths = root.path("route").path("paths");
        if (!paths.isArray() || paths.isEmpty()) {
            throw new RoutePlanningException("路径规划暂不可用：未返回可用路线");
        }
        JsonNode firstPath = paths.get(0);
        List<RoutePlanningPoint> points = parsePolyline(firstPath.path("steps"));
        if (points.size() < 2) {
            throw new RoutePlanningException("路径规划暂不可用：路线坐标不完整");
        }

        return RoutePlanningResult.builder()
                .provider(PROVIDER)
                .distanceMeters(parseLong(firstPath.path("distance").asText()))
                .durationSeconds(parseLong(firstPath.path("duration").asText()))
                .path(points)
                .build();
    }

    private List<RoutePlanningPoint> parsePolyline(JsonNode steps) {
        List<RoutePlanningPoint> points = new ArrayList<>();
        if (!steps.isArray()) {
            return points;
        }
        for (JsonNode step : steps) {
            String polyline = step.path("polyline").asText("");
            for (String pair : polyline.split(";")) {
                String[] values = pair.split(",");
                if (values.length != 2 || isBlank(values[0]) || isBlank(values[1])) {
                    continue;
                }
                RoutePlanningPoint point = RoutePlanningPoint.builder()
                        .longitude(new BigDecimal(values[0].trim()))
                        .latitude(new BigDecimal(values[1].trim()))
                        .build();
                if (points.isEmpty() || !samePoint(points.get(points.size() - 1), point)) {
                    points.add(point);
                }
            }
        }
        return points;
    }

    private boolean samePoint(RoutePlanningPoint left, RoutePlanningPoint right) {
        return left.getLongitude().compareTo(right.getLongitude()) == 0
                && left.getLatitude().compareTo(right.getLatitude()) == 0;
    }

    private Long parseLong(String value) {
        if (isBlank(value)) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String coordinate(BigDecimal longitude, BigDecimal latitude) {
        if (longitude == null || latitude == null) {
            throw new RoutePlanningException("路径规划暂不可用：坐标不完整");
        }
        return longitude.toPlainString() + "," + latitude.toPlainString();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
