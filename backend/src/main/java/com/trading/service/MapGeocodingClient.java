package com.trading.service;

import com.trading.dto.AddressValidationRequest;

import java.util.List;

public interface MapGeocodingClient {
    List<MapGeocodingResult> geocode(AddressValidationRequest request);
}
