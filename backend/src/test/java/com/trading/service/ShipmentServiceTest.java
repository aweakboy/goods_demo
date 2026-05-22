package com.trading.service;

import com.trading.common.BusinessException;
import com.trading.dto.ShipRequest;
import com.trading.dto.ShipmentResponse;
import com.trading.entity.*;
import com.trading.enums.AddressValidationStatus;
import com.trading.enums.OrderStatus;
import com.trading.enums.ShipmentStatus;
import com.trading.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ShipmentServiceTest {

    @Mock ShipmentRepository shipmentRepository;
    @Mock ShipmentEventRepository shipmentEventRepository;
    @Mock OrderRepository orderRepository;
    @Mock OrderItemRepository orderItemRepository;
    @Mock ProductRepository productRepository;
    @Mock ShopRepository shopRepository;
    @Mock ShipmentMapSimulationService shipmentMapSimulationService;
    @Mock ShipmentRoutePlanningService shipmentRoutePlanningService;

    @InjectMocks ShipmentService shipmentService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(shipmentService, "defaultDeliveryDays", 3);
    }

    @Test
    void createShipment_success_createsShipmentEventAndUpdatesOrder() {
        Order order = paidOrder();
        Product product = Product.builder().id(10L).sellerId(2L).build();
        Shop shop = shop();

        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(shipmentRepository.existsByOrderId(100L)).thenReturn(false);
        when(shipmentRepository.existsByCarrierCodeAndTrackingNumber("SF", "SF100")).thenReturn(false);
        when(shopRepository.findBySellerId(2L)).thenReturn(Optional.of(shop));
        when(shipmentRepository.save(any(Shipment.class))).thenAnswer(i -> {
            Shipment shipment = i.getArgument(0);
            shipment.setId(200L);
            return shipment;
        });
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));
        when(shipmentEventRepository.findByShipmentIdOrderByEventTimeAscIdAsc(200L))
                .thenReturn(List.of(ShipmentEvent.builder()
                        .id(300L)
                        .shipmentId(200L)
                        .status(ShipmentStatus.SHIPPED)
                        .description("卖家已发货，快递已揽收")
                        .eventTime(java.time.LocalDateTime.now())
                        .build()));
        when(shipmentMapSimulationService.build(any(Shipment.class))).thenReturn(
                com.trading.dto.ShipmentMapSimulationResponse.builder()
                        .routeAvailable(true)
                        .progress(new BigDecimal("0.05"))
                        .build()
        );
        when(shipmentRoutePlanningService.findSnapshot(200L)).thenReturn(Optional.empty());
        when(shipmentMapSimulationService.build(any(Shipment.class), isNull())).thenReturn(
                com.trading.dto.ShipmentMapSimulationResponse.builder()
                        .routeAvailable(true)
                        .progress(new BigDecimal("0.05"))
                        .build()
        );

        Order result = shipmentService.createShipment(100L, 2L, shipRequest());

        assertEquals(OrderStatus.SHIPPED, result.getStatus());
        assertEquals("SF100", result.getTrackingNumber());
        assertNotNull(result.getShipment());
        assertEquals("顺丰速运", result.getShipment().getCarrierName());
        assertTrue(result.getShipment().getMapSimulation().isRouteAvailable());
        verify(shipmentRepository).save(argThat(s ->
                s.getOrderId().equals(100L)
                        && s.getStatus() == ShipmentStatus.SHIPPED
                        && "测试店铺".equals(s.getOriginName())
                        && "浙江省杭州市西湖区文三路100号".equals(s.getDestinationFullAddress())
        ));
        verify(shipmentEventRepository).save(argThat(e ->
                e.getShipmentId().equals(200L) && e.getStatus() == ShipmentStatus.SHIPPED
        ));
        verify(shipmentRoutePlanningService).planAfterShipmentCreated(any(Shipment.class));
        verify(orderRepository).save(order);
    }

    @Test
    void createShipment_nonPaidOrder_throwsBadRequest() {
        Order order = paidOrder();
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> shipmentService.createShipment(100L, 2L, shipRequest()));

        assertEquals(400, ex.getStatus());
        verify(shipmentRepository, never()).save(any());
    }

    @Test
    void createShipment_wrongSeller_throwsForbidden() {
        Order order = paidOrder();
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
        when(productRepository.findById(10L)).thenReturn(Optional.of(Product.builder().id(10L).sellerId(9L).build()));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> shipmentService.createShipment(100L, 2L, shipRequest()));

        assertEquals(403, ex.getStatus());
        verify(shipmentRepository, never()).save(any());
    }

    @Test
    void createShipment_missingCarrier_throwsBadRequest() {
        ShipRequest request = shipRequest();
        request.setCarrierCode("");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> shipmentService.createShipment(100L, 2L, request));

        assertEquals(400, ex.getStatus());
        assertTrue(ex.getMessage().contains("物流公司"));
    }

    @Test
    void createShipment_duplicateShipment_throwsBadRequest() {
        Order order = paidOrder();
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
        when(productRepository.findById(10L)).thenReturn(Optional.of(Product.builder().id(10L).sellerId(2L).build()));
        when(shipmentRepository.existsByOrderId(100L)).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> shipmentService.createShipment(100L, 2L, shipRequest()));

        assertEquals(400, ex.getStatus());
        assertTrue(ex.getMessage().contains("已存在发货记录"));
    }

    @Test
    void createShipment_duplicateTracking_throwsBadRequest() {
        Order order = paidOrder();
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
        when(productRepository.findById(10L)).thenReturn(Optional.of(Product.builder().id(10L).sellerId(2L).build()));
        when(shipmentRepository.existsByCarrierCodeAndTrackingNumber("SF", "SF100")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> shipmentService.createShipment(100L, 2L, shipRequest()));

        assertEquals(400, ex.getStatus());
        assertTrue(ex.getMessage().contains("运单号已存在"));
    }

    @Test
    void getShipmentResponse_legacyTrackingNumber_returnsLegacySummary() {
        Order order = Order.builder()
                .id(100L)
                .trackingNumber("OLD100")
                .status(OrderStatus.SHIPPED)
                .build();
        when(shipmentRepository.findByOrderId(100L)).thenReturn(Optional.empty());

        ShipmentResponse response = shipmentService.getShipmentResponse(order);

        assertTrue(response.isLegacy());
        assertFalse(response.isTrackingAvailable());
        assertEquals("OLD100", response.getTrackingNumber());
        assertFalse(response.getMapSimulation().isRouteAvailable());
    }

    @Test
    void getShipmentResponse_existingShipment_doesNotTriggerRoutePlanning() {
        Order order = Order.builder().id(100L).status(OrderStatus.SHIPPED).build();
        Shipment shipment = Shipment.builder()
                .id(200L)
                .orderId(100L)
                .carrierCode("SF")
                .carrierName("顺丰速运")
                .trackingNumber("SF100")
                .status(ShipmentStatus.SHIPPED)
                .originLongitude(new BigDecimal("120.0000000"))
                .originLatitude(new BigDecimal("30.0000000"))
                .destinationLongitude(new BigDecimal("121.0000000"))
                .destinationLatitude(new BigDecimal("31.0000000"))
                .build();
        when(shipmentRepository.findByOrderId(100L)).thenReturn(Optional.of(shipment));
        when(shipmentEventRepository.findByShipmentIdOrderByEventTimeAscIdAsc(200L)).thenReturn(List.of());
        when(shipmentRoutePlanningService.findSnapshot(200L)).thenReturn(Optional.empty());
        when(shipmentMapSimulationService.build(eq(shipment), isNull())).thenReturn(
                com.trading.dto.ShipmentMapSimulationResponse.builder().routeAvailable(true).build()
        );

        ShipmentResponse response = shipmentService.getShipmentResponse(order);

        assertFalse(response.isLegacy());
        verify(shipmentRoutePlanningService, never()).planAfterShipmentCreated(any());
        verify(shipmentRoutePlanningService, never()).refreshRoute(anyLong());
    }

    private ShipRequest shipRequest() {
        ShipRequest request = new ShipRequest();
        request.setCarrierCode("SF");
        request.setCarrierName("顺丰速运");
        request.setTrackingNumber("SF100");
        return request;
    }

    private Order paidOrder() {
        return Order.builder()
                .id(100L)
                .buyerId(1L)
                .status(OrderStatus.PAID)
                .address("浙江省杭州市西湖区文三路100号")
                .receiverName("张三")
                .receiverPhone("13800138000")
                .receiverProvince("浙江省")
                .receiverCity("杭州市")
                .receiverDistrict("西湖区")
                .receiverDetailAddress("文三路100号")
                .receiverFullAddress("浙江省杭州市西湖区文三路100号")
                .receiverLongitude(new BigDecimal("120.1234567"))
                .receiverLatitude(new BigDecimal("30.1234567"))
                .receiverAddressValidationStatus(AddressValidationStatus.VALID)
                .items(List.of(OrderItem.builder().productId(10L).quantity(1).build()))
                .build();
    }

    private Shop shop() {
        return Shop.builder()
                .id(20L)
                .sellerId(2L)
                .name("测试店铺")
                .province("浙江省")
                .city("杭州市")
                .district("西湖区")
                .detailAddress("教工路1号")
                .fullAddress("浙江省杭州市西湖区教工路1号")
                .longitude(new BigDecimal("120.1111111"))
                .latitude(new BigDecimal("30.1111111"))
                .addressValidationStatus(AddressValidationStatus.VALID)
                .build();
    }
}
