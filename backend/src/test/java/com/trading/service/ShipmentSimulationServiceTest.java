package com.trading.service;

import com.trading.common.BusinessException;
import com.trading.dto.ShipmentMapSimulationResponse;
import com.trading.dto.ShipmentResponse;
import com.trading.entity.Shipment;
import com.trading.enums.ShipmentStatus;
import com.trading.repository.ShipmentEventRepository;
import com.trading.repository.ShipmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ShipmentSimulationServiceTest {

    @Mock ShipmentRepository shipmentRepository;
    @Mock ShipmentEventRepository shipmentEventRepository;
    @Mock ShipmentService shipmentService;

    @InjectMocks ShipmentSimulationService simulationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(shipmentRepository.save(any(Shipment.class))).thenAnswer(i -> i.getArgument(0));
        when(shipmentService.toResponse(any(Shipment.class))).thenAnswer(i -> {
            Shipment shipment = i.getArgument(0);
            return ShipmentResponse.builder()
                    .id(shipment.getId())
                    .status(shipment.getStatus().name())
                    .mapSimulation(ShipmentMapSimulationResponse.builder()
                            .routeAvailable(true)
                            .status(shipment.getStatus().name())
                            .build())
                    .build();
        });
    }

    @Test
    void advance_fullChain_updatesStatusAndWritesEvents() {
        ReflectionTestUtils.setField(simulationService, "simulationEnabled", true);
        Shipment shipment = Shipment.builder()
                .id(1L)
                .status(ShipmentStatus.SHIPPED)
                .originCity("杭州")
                .destinationCity("上海")
                .destinationDistrict("浦东新区")
                .build();
        when(shipmentRepository.findById(1L)).thenReturn(Optional.of(shipment));

        assertEquals("IN_TRANSIT", simulationService.advance(1L).getStatus());
        assertEquals("OUT_FOR_DELIVERY", simulationService.advance(1L).getStatus());
        ShipmentResponse delivered = simulationService.advance(1L);
        assertEquals("DELIVERED", delivered.getStatus());
        assertNotNull(delivered.getMapSimulation());
        assertEquals("DELIVERED", delivered.getMapSimulation().getStatus());
        assertNotNull(shipment.getDeliveredAt());
        verify(shipmentEventRepository, times(3)).save(any());
    }

    @Test
    void advance_simulationDisabled_doesNotModifyShipment() {
        ReflectionTestUtils.setField(simulationService, "simulationEnabled", false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> simulationService.advance(1L));

        assertEquals(400, ex.getStatus());
        verify(shipmentRepository, never()).save(any());
    }

    @Test
    void advance_terminalStatus_throwsBadRequest() {
        ReflectionTestUtils.setField(simulationService, "simulationEnabled", true);
        Shipment shipment = Shipment.builder().id(1L).status(ShipmentStatus.DELIVERED).build();
        when(shipmentRepository.findById(1L)).thenReturn(Optional.of(shipment));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> simulationService.advance(1L));

        assertEquals(400, ex.getStatus());
        assertTrue(ex.getMessage().contains("物流已结束"));
    }

    @Test
    void markException_updatesStatusAndWritesEvent() {
        ReflectionTestUtils.setField(simulationService, "simulationEnabled", true);
        Shipment shipment = Shipment.builder().id(1L).status(ShipmentStatus.IN_TRANSIT).destinationCity("上海").build();
        when(shipmentRepository.findById(1L)).thenReturn(Optional.of(shipment));

        ShipmentResponse response = simulationService.markException(1L, "地址无法送达");

        assertEquals("EXCEPTION", response.getStatus());
        assertEquals(ShipmentStatus.EXCEPTION, shipment.getStatus());
        verify(shipmentEventRepository).save(argThat(e -> e.getDescription().contains("地址无法送达")));
    }
}
