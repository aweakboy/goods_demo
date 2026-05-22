package com.trading.service;

import com.trading.dto.RoutePlanningRequest;
import com.trading.dto.RoutePlanningResult;

public interface RoutePlanningClient {
    RoutePlanningResult plan(RoutePlanningRequest request);
}
