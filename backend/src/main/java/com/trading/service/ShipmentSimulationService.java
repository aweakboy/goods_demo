package com.trading.service;

import com.trading.common.BusinessException;
import com.trading.dto.ShipmentResponse;
import com.trading.entity.Shipment;
import com.trading.entity.ShipmentEvent;
import com.trading.enums.ShipmentStatus;
import com.trading.repository.ShipmentEventRepository;
import com.trading.repository.ShipmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ShipmentSimulationService {

    private final ShipmentRepository shipmentRepository;
    private final ShipmentEventRepository shipmentEventRepository;
    private final ShipmentService shipmentService;

    @Value("${shipping.simulation.enabled:false}")
    private boolean simulationEnabled;

    @Transactional
    public ShipmentResponse advance(Long shipmentId) {
        ensureEnabled();
        Shipment shipment = getShipment(shipmentId);
        LocalDateTime now = LocalDateTime.now();
        switch (shipment.getStatus()) {
            case SHIPPED -> updateStatus(shipment, ShipmentStatus.IN_TRANSIT, now,
                    firstNonBlank(shipment.getOriginCity(), shipment.getOriginFullAddress()),
                    "包裹已离开发货地，正在运输中");
            case IN_TRANSIT -> updateStatus(shipment, ShipmentStatus.OUT_FOR_DELIVERY, now,
                    firstNonBlank(shipment.getDestinationCity(), shipment.getDestinationFullAddress()),
                    "包裹已到达目的地附近站点，正在安排派送");
            case OUT_FOR_DELIVERY -> {
                shipment.setDeliveredAt(now);
                updateStatus(shipment, ShipmentStatus.DELIVERED, now,
                        firstNonBlank(shipment.getDestinationDistrict(), shipment.getDestinationFullAddress()),
                        "包裹已签收");
            }
            case DELIVERED, EXCEPTION -> throw BusinessException.badRequest("物流已结束，无法继续推进");
        }
        return shipmentService.toResponse(shipment);
    }

    @Transactional
    public ShipmentResponse markException(Long shipmentId, String reason) {
        ensureEnabled();
        if (!hasText(reason)) {
            throw BusinessException.badRequest("请填写异常原因");
        }
        Shipment shipment = getShipment(shipmentId);
        if (shipment.getStatus() == ShipmentStatus.DELIVERED || shipment.getStatus() == ShipmentStatus.EXCEPTION) {
            throw BusinessException.badRequest("物流已结束，无法标记异常");
        }
        updateStatus(shipment, ShipmentStatus.EXCEPTION, LocalDateTime.now(),
                firstNonBlank(shipment.getDestinationCity(), shipment.getOriginCity()),
                "物流异常：" + reason.trim());
        return shipmentService.toResponse(shipment);
    }

    private Shipment getShipment(Long shipmentId) {
        return shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> BusinessException.notFound("发货记录不存在"));
    }

    private void updateStatus(Shipment shipment, ShipmentStatus status, LocalDateTime eventTime,
                              String location, String description) {
        shipment.setStatus(status);
        shipmentRepository.save(shipment);
        shipmentEventRepository.save(ShipmentEvent.builder()
                .shipmentId(shipment.getId())
                .status(status)
                .eventTime(eventTime)
                .location(location)
                .description(description)
                .build());
    }

    private void ensureEnabled() {
        if (!simulationEnabled) {
            throw BusinessException.badRequest("模拟物流未开启");
        }
    }

    private String firstNonBlank(String first, String second) {
        return hasText(first) ? first : second;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
