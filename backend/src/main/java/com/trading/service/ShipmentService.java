package com.trading.service;

import com.trading.common.BusinessException;
import com.trading.dto.CarrierOptionResponse;
import com.trading.dto.ShipRequest;
import com.trading.dto.ShipmentResponse;
import com.trading.entity.*;
import com.trading.enums.OrderStatus;
import com.trading.enums.ShipmentStatus;
import com.trading.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ShipmentService {

    private static final List<CarrierOptionResponse> CARRIER_OPTIONS = List.of(
            new CarrierOptionResponse("SF", "顺丰速运"),
            new CarrierOptionResponse("ZTO", "中通快递"),
            new CarrierOptionResponse("YTO", "圆通速递"),
            new CarrierOptionResponse("YD", "韵达快递"),
            new CarrierOptionResponse("STO", "申通快递"),
            new CarrierOptionResponse("JD", "京东物流"),
            new CarrierOptionResponse("EMS", "邮政EMS")
    );

    private static final Map<String, String> CARRIER_NAME_BY_CODE = CARRIER_OPTIONS.stream()
            .collect(java.util.stream.Collectors.toUnmodifiableMap(CarrierOptionResponse::getCode, CarrierOptionResponse::getName));

    private final ShipmentRepository shipmentRepository;
    private final ShipmentEventRepository shipmentEventRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final ShopRepository shopRepository;
    private final ShipmentMapSimulationService shipmentMapSimulationService;
    private final ShipmentRoutePlanningService shipmentRoutePlanningService;

    @Value("${shipping.default-delivery-days:3}")
    private int defaultDeliveryDays;

    public List<CarrierOptionResponse> getCarrierOptions() {
        return CARRIER_OPTIONS;
    }

    @Transactional
    public Order createShipment(Long orderId, Long sellerId, ShipRequest request) {
        ShipRequest normalized = normalize(request);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> BusinessException.notFound("订单不存在"));
        if (order.getStatus() != OrderStatus.PAID) {
            throw BusinessException.badRequest("只能对已付款订单发货");
        }
        if (!containsSellerProduct(order, sellerId)) {
            throw BusinessException.forbidden("无权发货该订单");
        }
        if (shipmentRepository.existsByOrderId(orderId)) {
            throw BusinessException.badRequest("订单已存在发货记录");
        }
        if (shipmentRepository.existsByCarrierCodeAndTrackingNumber(
                normalized.getCarrierCode(), normalized.getTrackingNumber())) {
            throw BusinessException.badRequest("该物流公司下的运单号已存在");
        }

        Shop shop = shopRepository.findBySellerId(sellerId)
                .orElseThrow(() -> BusinessException.badRequest("请先注册店铺"));
        LocalDateTime now = LocalDateTime.now();
        Shipment shipment = Shipment.builder()
                .orderId(orderId)
                .carrierCode(normalized.getCarrierCode())
                .carrierName(normalized.getCarrierName())
                .trackingNumber(normalized.getTrackingNumber())
                .status(ShipmentStatus.SHIPPED)
                .originName(shop.getName())
                .originProvince(shop.getProvince())
                .originCity(shop.getCity())
                .originDistrict(shop.getDistrict())
                .originDetailAddress(shop.getDetailAddress())
                .originFullAddress(firstNonBlank(shop.getFullAddress(), buildFullAddress(
                        shop.getProvince(), shop.getCity(), shop.getDistrict(), shop.getDetailAddress())))
                .originLongitude(shop.getLongitude())
                .originLatitude(shop.getLatitude())
                .originAddressValidationStatus(shop.getAddressValidationStatus())
                .destinationName(order.getReceiverName())
                .destinationPhone(order.getReceiverPhone())
                .destinationProvince(order.getReceiverProvince())
                .destinationCity(order.getReceiverCity())
                .destinationDistrict(order.getReceiverDistrict())
                .destinationDetailAddress(order.getReceiverDetailAddress())
                .destinationFullAddress(firstNonBlank(order.getReceiverFullAddress(), order.getAddress()))
                .destinationLongitude(order.getReceiverLongitude())
                .destinationLatitude(order.getReceiverLatitude())
                .destinationAddressValidationStatus(order.getReceiverAddressValidationStatus())
                .shippedAt(now)
                .estimatedDeliveredAt(now.plusDays(defaultDeliveryDays))
                .build();
        shipment = shipmentRepository.save(shipment);
        if (shipmentRoutePlanningService != null) {
            shipmentRoutePlanningService.planAfterShipmentCreated(shipment);
        }
        shipmentEventRepository.save(ShipmentEvent.builder()
                .shipmentId(shipment.getId())
                .status(ShipmentStatus.SHIPPED)
                .eventTime(now)
                .location(firstNonBlank(shipment.getOriginCity(), shipment.getOriginFullAddress()))
                .description("卖家已发货，快递已揽收")
                .build());

        order.setStatus(OrderStatus.SHIPPED);
        order.setTrackingNumber(shipment.getTrackingNumber());
        order = orderRepository.save(order);
        order.setShipment(toResponse(shipment));
        return order;
    }

    public ShipmentResponse getShipmentResponse(Order order) {
        if (order == null || order.getId() == null) {
            return null;
        }
        Optional<Shipment> shipment = shipmentRepository.findByOrderId(order.getId());
        if (shipment.isPresent()) {
            return toResponse(shipment.get());
        }
        if (hasText(order.getTrackingNumber())) {
            return ShipmentResponse.legacy(order.getId(), order.getTrackingNumber());
        }
        return null;
    }

    public ShipmentResponse toResponse(Shipment shipment) {
        var routeSnapshot = shipmentRoutePlanningService != null
                ? shipmentRoutePlanningService.findSnapshot(shipment.getId())
                : Optional.<ShipmentRouteSnapshot>empty();
        if (routeSnapshot == null) {
            routeSnapshot = Optional.empty();
        }
        return ShipmentResponse.from(
                shipment,
                shipmentEventRepository.findByShipmentIdOrderByEventTimeAscIdAsc(shipment.getId()),
                shipmentMapSimulationService.build(shipment, routeSnapshot.orElse(null))
        );
    }

    @Transactional
    public ShipmentResponse refreshRoute(Long shipmentId) {
        if (shipmentRoutePlanningService == null) {
            throw BusinessException.badRequest("路径规划服务未配置");
        }
        shipmentRoutePlanningService.refreshRoute(shipmentId);
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> BusinessException.notFound("发货记录不存在"));
        return toResponse(shipment);
    }

    public Order attachShipment(Order order) {
        if (order != null) {
            order.setShipment(getShipmentResponse(order));
        }
        return order;
    }

    public List<Order> attachShipments(List<Order> orders) {
        orders.forEach(this::attachShipment);
        return orders;
    }

    private ShipRequest normalize(ShipRequest request) {
        if (request == null) {
            throw BusinessException.badRequest("请填写物流信息");
        }
        String carrierCode = trim(request.getCarrierCode());
        String carrierName = trim(request.getCarrierName());
        String trackingNumber = trim(request.getTrackingNumber());
        if (!hasText(carrierCode)) {
            throw BusinessException.badRequest("请选择物流公司");
        }
        carrierCode = carrierCode.toUpperCase();
        if (!hasText(carrierName)) {
            carrierName = CARRIER_NAME_BY_CODE.get(carrierCode);
        }
        if (!hasText(carrierName)) {
            throw BusinessException.badRequest("请选择物流公司");
        }
        if (!hasText(trackingNumber)) {
            throw BusinessException.badRequest("请填写物流单号");
        }
        ShipRequest normalized = new ShipRequest();
        normalized.setCarrierCode(carrierCode);
        normalized.setCarrierName(carrierName);
        normalized.setTrackingNumber(trackingNumber);
        return normalized;
    }

    private boolean containsSellerProduct(Order order, Long sellerId) {
        List<OrderItem> items = order.getItems();
        if (items == null || items.isEmpty()) {
            items = orderItemRepository.findByOrderId(order.getId());
        }
        return items.stream()
                .map(OrderItem::getProductId)
                .map(productRepository::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .anyMatch(product -> sellerId.equals(product.getSellerId()));
    }

    private String buildFullAddress(String province, String city, String district, String detailAddress) {
        String value = String.join("", nullToEmpty(province), nullToEmpty(city), nullToEmpty(district), nullToEmpty(detailAddress));
        return hasText(value) ? value : null;
    }

    private String firstNonBlank(String first, String second) {
        return hasText(first) ? first : second;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
