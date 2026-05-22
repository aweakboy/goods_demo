package com.trading.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trading.config.MapServiceConfig;
import com.trading.config.RoutePlanningConfig;
import com.trading.dto.RoutePlanningRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AmapRoutePlanningClientTest {

    @Test
    void plan_success_parsesDistanceDurationAndPolyline() {
        TestClient client = client(response(200, """
                {"status":"1","info":"OK","route":{"paths":[{"distance":"1234","duration":"600","steps":[
                {"polyline":"120.000000,30.000000;120.100000,30.100000"},
                {"polyline":"120.100000,30.100000;120.200000,30.200000"}
                ]}]}}
                """));

        var result = client.plan(request());

        assertEquals("AMAP", result.getProvider());
        assertEquals(1234L, result.getDistanceMeters());
        assertEquals(600L, result.getDurationSeconds());
        assertEquals(3, result.getPath().size());
        assertEquals(new BigDecimal("120.200000"), result.getPath().get(2).getLongitude());
    }

    @Test
    void plan_emptyRoute_throwsRoutePlanningException() {
        TestClient client = client(response(200, """
                {"status":"1","info":"OK","route":{"paths":[]}}
                """));

        RoutePlanningException ex = assertThrows(RoutePlanningException.class, () -> client.plan(request()));

        assertTrue(ex.getMessage().contains("未返回可用路线"));
    }

    @Test
    void plan_providerFailure_throwsRoutePlanningException() {
        TestClient client = client(response(200, """
                {"status":"0","info":"INVALID_USER_KEY"}
                """));

        RoutePlanningException ex = assertThrows(RoutePlanningException.class, () -> client.plan(request()));

        assertTrue(ex.getMessage().contains("INVALID_USER_KEY"));
    }

    @Test
    void plan_httpError_throwsRoutePlanningException() {
        TestClient client = client(response(500, "server error"));

        RoutePlanningException ex = assertThrows(RoutePlanningException.class, () -> client.plan(request()));

        assertTrue(ex.getMessage().contains("响应异常"));
    }

    @Test
    void plan_timeout_throwsRoutePlanningException() {
        TestClient client = client(new java.net.http.HttpTimeoutException("timeout"));

        RoutePlanningException ex = assertThrows(RoutePlanningException.class, () -> client.plan(request()));

        assertTrue(ex.getMessage().contains("路径规划暂不可用"));
    }

    private TestClient client(HttpResponse<String> response) {
        return new TestClient(mapConfig(), routeConfig(), new ObjectMapper(), response, null);
    }

    private TestClient client(Exception failure) {
        return new TestClient(mapConfig(), routeConfig(), new ObjectMapper(), null, failure);
    }

    private MapServiceConfig mapConfig() {
        MapServiceConfig config = new MapServiceConfig();
        config.setEnabled(true);
        config.setWebServiceKey("test-key");
        return config;
    }

    private RoutePlanningConfig routeConfig() {
        RoutePlanningConfig config = new RoutePlanningConfig();
        config.setEnabled(true);
        config.setStrategy("0");
        config.setConnectTimeoutMs(100);
        config.setReadTimeoutMs(100);
        return config;
    }

    private RoutePlanningRequest request() {
        return RoutePlanningRequest.builder()
                .originLongitude(new BigDecimal("120.0000000"))
                .originLatitude(new BigDecimal("30.0000000"))
                .destinationLongitude(new BigDecimal("121.0000000"))
                .destinationLatitude(new BigDecimal("31.0000000"))
                .build();
    }

    @SuppressWarnings("unchecked")
    private HttpResponse<String> response(int status, String body) {
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(status);
        when(response.body()).thenReturn(body);
        return response;
    }

    private static class TestClient extends AmapRoutePlanningClient {
        private final HttpResponse<String> response;
        private final Exception failure;

        TestClient(MapServiceConfig mapConfig, RoutePlanningConfig routeConfig, ObjectMapper objectMapper,
                   HttpResponse<String> response, Exception failure) {
            super(mapConfig, routeConfig, objectMapper);
            this.response = response;
            this.failure = failure;
        }

        @Override
        protected HttpResponse<String> send(URI uri) throws Exception {
            if (failure != null) {
                throw failure;
            }
            return response;
        }
    }
}
