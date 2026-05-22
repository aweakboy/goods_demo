package com.trading.service;

import com.trading.common.BusinessException;
import com.trading.dto.AddressValidationRequest;
import com.trading.dto.AddressValidationResult;
import com.trading.enums.AddressValidationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class AddressValidationServiceTest {

    @Mock MapGeocodingClient mapGeocodingClient;

    AddressValidationService addressValidationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        addressValidationService = new AddressValidationService(mapGeocodingClient);
    }

    @Test
    void validate_success_returnsValidCoordinate() {
        when(mapGeocodingClient.geocode(any())).thenReturn(List.of(validResult()));

        AddressValidationResult result = addressValidationService.validateOrThrow(request());

        assertEquals(AddressValidationStatus.VALID, result.getStatus());
        assertEquals(new BigDecimal("120.1234567"), result.getLongitude());
        assertEquals(new BigDecimal("30.1234567"), result.getLatitude());
    }

    @Test
    void validate_noResult_throwsBadRequest() {
        when(mapGeocodingClient.geocode(any())).thenReturn(List.of());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> addressValidationService.validateOrThrow(request()));

        assertEquals(400, ex.getStatus());
        assertTrue(ex.getMessage().contains("地址无法定位"));
    }

    @Test
    void validate_areaMismatch_throwsBadRequest() {
        MapGeocodingResult result = validResult();
        result.setDistrict("拱墅区");
        when(mapGeocodingClient.geocode(any())).thenReturn(List.of(result));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> addressValidationService.validateOrThrow(request()));

        assertEquals(400, ex.getStatus());
        assertTrue(ex.getMessage().contains("地址与行政区不匹配"));
    }

    @Test
    void validate_mapUnavailable_throwsBadRequest() {
        when(mapGeocodingClient.geocode(any()))
                .thenThrow(new MapServiceUnavailableException("地图服务未配置"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> addressValidationService.validateOrThrow(request()));

        assertEquals(400, ex.getStatus());
        assertTrue(ex.getMessage().contains("地址校验暂不可用"));
    }

    private AddressValidationRequest request() {
        return new AddressValidationRequest("浙江省", "杭州市", "西湖区", "文三路100号");
    }

    private MapGeocodingResult validResult() {
        return MapGeocodingResult.builder()
                .formattedAddress("浙江省杭州市西湖区文三路100号")
                .province("浙江省")
                .city("杭州市")
                .district("西湖区")
                .level("门牌号")
                .longitude(new BigDecimal("120.1234567"))
                .latitude(new BigDecimal("30.1234567"))
                .build();
    }
}
