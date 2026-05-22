package com.trading.service;

import com.trading.dto.BuyerNotificationResponse;
import com.trading.entity.BuyerNotification;
import com.trading.enums.BuyerNotificationType;
import com.trading.repository.BuyerNotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BuyerNotificationServiceTest {

    @Mock BuyerNotificationRepository buyerNotificationRepository;

    @InjectMocks BuyerNotificationService buyerNotificationService;

    @Test
    void countUnread_returnsBuyerUnreadCount() {
        when(buyerNotificationRepository.countByBuyerIdAndReadAtIsNull(2L)).thenReturn(3L);

        assertEquals(3L, buyerNotificationService.countUnread(2L));
    }

    @Test
    void listNotifications_returnsCurrentBuyerNotifications() {
        when(buyerNotificationRepository.findByBuyerIdOrderByCreatedAtDesc(eq(2L), any()))
                .thenReturn(new PageImpl<>(List.of(notification())));

        assertEquals(1, buyerNotificationService.listNotifications(2L, 0, 20).getTotalElements());
    }

    @Test
    void markRead_setsReadAtOnce() {
        BuyerNotification notification = notification();
        when(buyerNotificationRepository.findByIdAndBuyerId(1L, 2L)).thenReturn(Optional.of(notification));
        when(buyerNotificationRepository.save(any(BuyerNotification.class))).thenAnswer(i -> i.getArgument(0));

        BuyerNotificationResponse response = buyerNotificationService.markRead(2L, 1L);

        assertTrue(response.getRead());
        assertNotNull(notification.getReadAt());
        verify(buyerNotificationRepository).save(notification);
    }

    @Test
    void markAllRead_updatesOnlyCurrentBuyerUnreadNotifications() {
        BuyerNotification first = notification();
        BuyerNotification second = notification();
        second.setId(2L);
        when(buyerNotificationRepository.findByBuyerIdAndReadAtIsNull(2L)).thenReturn(List.of(first, second));

        long updated = buyerNotificationService.markAllRead(2L);

        assertEquals(2L, updated);
        assertNotNull(first.getReadAt());
        assertNotNull(second.getReadAt());
        verify(buyerNotificationRepository).saveAll(List.of(first, second));
    }

    private BuyerNotification notification() {
        return BuyerNotification.builder()
                .id(1L)
                .buyerId(2L)
                .type(BuyerNotificationType.PRICE_DROP)
                .title("商品降价提醒")
                .content("商品A 已降价")
                .productId(10L)
                .shopId(20L)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
