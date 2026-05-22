package com.trading.service;

import com.trading.common.BusinessException;
import com.trading.entity.BuyerNotification;
import com.trading.entity.PriceAlert;
import com.trading.entity.Product;
import com.trading.entity.Shop;
import com.trading.enums.PriceAlertStatus;
import com.trading.enums.ProductStatus;
import com.trading.repository.BuyerNotificationRepository;
import com.trading.repository.PriceAlertRepository;
import com.trading.repository.ProductRepository;
import com.trading.repository.ShopRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PriceAlertServiceTest {

    @Mock PriceAlertRepository priceAlertRepository;
    @Mock BuyerNotificationRepository buyerNotificationRepository;
    @Mock ProductRepository productRepository;
    @Mock ShopRepository shopRepository;

    @InjectMocks PriceAlertService priceAlertService;

    @Test
    void createOrUpdateAlert_validTarget_createsActiveAlert() {
        Product product = product();
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(priceAlertRepository.findByBuyerIdAndProductId(2L, 10L)).thenReturn(Optional.empty());
        when(priceAlertRepository.save(any(PriceAlert.class))).thenAnswer(i -> {
            PriceAlert alert = i.getArgument(0);
            alert.setId(1L);
            return alert;
        });

        PriceAlert alert = priceAlertService.createOrUpdateAlert(2L, 10L, BigDecimal.valueOf(80));

        assertEquals(PriceAlertStatus.ACTIVE, alert.getStatus());
        assertEquals(BigDecimal.valueOf(80), alert.getTargetPrice());
        verify(priceAlertRepository).save(any(PriceAlert.class));
    }

    @Test
    void createOrUpdateAlert_existingAlert_updatesAndReactivates() {
        Product product = product();
        PriceAlert existing = alert(BigDecimal.valueOf(70), PriceAlertStatus.TRIGGERED);
        existing.setLastNotifiedPrice(BigDecimal.valueOf(70));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(priceAlertRepository.findByBuyerIdAndProductId(2L, 10L)).thenReturn(Optional.of(existing));
        when(priceAlertRepository.save(any(PriceAlert.class))).thenAnswer(i -> i.getArgument(0));

        PriceAlert alert = priceAlertService.createOrUpdateAlert(2L, 10L, BigDecimal.valueOf(90));

        assertSame(existing, alert);
        assertEquals(PriceAlertStatus.ACTIVE, alert.getStatus());
        assertEquals(BigDecimal.valueOf(90), alert.getTargetPrice());
        assertNull(alert.getLastNotifiedPrice());
    }

    @Test
    void createOrUpdateAlert_targetNotBelowCurrentPrice_throwsBadRequest() {
        when(productRepository.findById(10L)).thenReturn(Optional.of(product()));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> priceAlertService.createOrUpdateAlert(2L, 10L, BigDecimal.valueOf(100)));

        assertEquals(400, ex.getStatus());
        verify(priceAlertRepository, never()).save(any());
    }

    @Test
    void processPriceChange_reachesTarget_createsNotificationAndTriggersAlert() {
        Product product = product();
        PriceAlert alert = alert(BigDecimal.valueOf(80), PriceAlertStatus.ACTIVE);
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(shopRepository.findBySellerId(20L)).thenReturn(Optional.of(shop()));
        when(priceAlertRepository.findByProductIdAndStatusAndTargetPriceGreaterThanEqual(
                10L, PriceAlertStatus.ACTIVE, BigDecimal.valueOf(80))).thenReturn(List.of(alert));

        int triggered = priceAlertService.processPriceChange(10L, BigDecimal.valueOf(100), BigDecimal.valueOf(80));

        assertEquals(1, triggered);
        assertEquals(PriceAlertStatus.TRIGGERED, alert.getStatus());
        assertEquals(BigDecimal.valueOf(80), alert.getLastNotifiedPrice());
        assertNotNull(alert.getTriggeredAt());
        verify(buyerNotificationRepository).save(any(BuyerNotification.class));
        verify(priceAlertRepository).save(alert);
    }

    @Test
    void processPriceChange_notReachedOrNotDecreased_doesNotNotify() {
        int notDecreased = priceAlertService.processPriceChange(10L, BigDecimal.valueOf(100), BigDecimal.valueOf(100));
        assertEquals(0, notDecreased);

        when(productRepository.findById(10L)).thenReturn(Optional.of(product()));
        when(priceAlertRepository.findByProductIdAndStatusAndTargetPriceGreaterThanEqual(
                10L, PriceAlertStatus.ACTIVE, BigDecimal.valueOf(90))).thenReturn(List.of());

        int notReached = priceAlertService.processPriceChange(10L, BigDecimal.valueOf(100), BigDecimal.valueOf(90));

        assertEquals(0, notReached);
        verify(buyerNotificationRepository, never()).save(any());
    }

    @Test
    void processPriceChange_triggeredOrCancelledAlertsAreIgnoredByActiveQuery() {
        when(productRepository.findById(10L)).thenReturn(Optional.of(product()));
        when(priceAlertRepository.findByProductIdAndStatusAndTargetPriceGreaterThanEqual(
                10L, PriceAlertStatus.ACTIVE, BigDecimal.valueOf(80))).thenReturn(List.of());

        int triggered = priceAlertService.processPriceChange(10L, BigDecimal.valueOf(100), BigDecimal.valueOf(80));

        assertEquals(0, triggered);
        verify(buyerNotificationRepository, never()).save(any());
    }

    @Test
    void listAlerts_usesCurrentBuyerOnly() {
        PriceAlert alert = alert(BigDecimal.valueOf(80), PriceAlertStatus.ACTIVE);
        alert.setProduct(product());
        when(priceAlertRepository.findByBuyerIdOrderByUpdatedAtDesc(eq(2L), any()))
                .thenReturn(new PageImpl<>(List.of(alert)));

        assertEquals(1, priceAlertService.listAlerts(2L, 0, 20).getTotalElements());
    }

    private Product product() {
        return Product.builder()
                .id(10L)
                .sellerId(20L)
                .name("商品A")
                .price(BigDecimal.valueOf(100))
                .stock(10)
                .status(ProductStatus.ACTIVE)
                .build();
    }

    private Shop shop() {
        return Shop.builder()
                .id(30L)
                .sellerId(20L)
                .name("测试店铺")
                .status(ProductStatus.ACTIVE)
                .build();
    }

    private PriceAlert alert(BigDecimal targetPrice, PriceAlertStatus status) {
        return PriceAlert.builder()
                .id(1L)
                .buyerId(2L)
                .productId(10L)
                .targetPrice(targetPrice)
                .status(status)
                .build();
    }
}
