package com.trading.service;

import com.trading.common.BusinessException;
import com.trading.dto.AddressValidationRequest;
import com.trading.dto.AddressValidationResult;
import com.trading.enums.AddressValidationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AddressValidationService {

    private final MapGeocodingClient mapGeocodingClient;

    public AddressValidationResult validateOrThrow(AddressValidationRequest request) {
        List<MapGeocodingResult> results;
        try {
            results = mapGeocodingClient.geocode(request);
        } catch (MapServiceUnavailableException e) {
            throw BusinessException.badRequest("地址校验暂不可用，请稍后重试");
        }

        if (results == null || results.isEmpty()) {
            throw BusinessException.badRequest("地址无法定位，请检查后重新填写");
        }

        return results.stream()
                .filter(result -> hasCoordinate(result))
                .filter(result -> administrativeAreaMatches(request, result))
                .max(Comparator.comparingInt(this::precisionScore))
                .map(this::toValidResult)
                .orElseThrow(() -> BusinessException.badRequest("地址与行政区不匹配，请检查后重新填写"));
    }

    private AddressValidationResult toValidResult(MapGeocodingResult result) {
        return AddressValidationResult.builder()
                .status(AddressValidationStatus.VALID)
                .formattedAddress(result.getFormattedAddress())
                .longitude(result.getLongitude())
                .latitude(result.getLatitude())
                .province(result.getProvince())
                .city(result.getCity())
                .district(result.getDistrict())
                .adcode(result.getAdcode())
                .level(result.getLevel())
                .build();
    }

    private boolean hasCoordinate(MapGeocodingResult result) {
        return result.getLongitude() != null && result.getLatitude() != null;
    }

    private boolean administrativeAreaMatches(AddressValidationRequest request, MapGeocodingResult result) {
        return partMatches(request.getProvince(), result.getProvince(), result.getFormattedAddress())
                && partMatches(request.getCity(), result.getCity(), result.getFormattedAddress())
                && partMatches(request.getDistrict(), result.getDistrict(), result.getFormattedAddress());
    }

    private boolean partMatches(String expected, String actual, String formattedAddress) {
        if (isBlank(expected)) {
            return true;
        }
        String expectedNorm = normalize(expected);
        String actualNorm = normalize(actual);
        String formattedNorm = normalize(formattedAddress);
        if (!isBlank(actualNorm)) {
            return actualNorm.contains(expectedNorm) || expectedNorm.contains(actualNorm);
        }
        return !isBlank(formattedNorm) && formattedNorm.contains(expectedNorm);
    }

    private int precisionScore(MapGeocodingResult result) {
        String level = normalize(result.getLevel());
        if (level.contains("门") || level.contains("号") || level.contains("poi") || level.contains("兴趣")) return 5;
        if (level.contains("道路") || level.contains("路") || level.contains("街")) return 4;
        if (level.contains("村") || level.contains("乡") || level.contains("镇")) return 3;
        if (level.contains("区") || level.contains("县")) return 2;
        if (level.contains("市")) return 1;
        return 0;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim()
                .replace(" ", "")
                .replace("省", "")
                .replace("市", "")
                .replace("区", "")
                .replace("县", "")
                .replace("特别行政", "")
                .replace("自治区", "")
                .replace("壮族", "")
                .replace("回族", "")
                .replace("维吾尔", "")
                .replace("自治州", "")
                .replace("自治县", "")
                .toLowerCase();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
