package com.trading.service;

import com.trading.common.BusinessException;
import com.trading.dto.BuyerNotificationResponse;
import com.trading.entity.BuyerNotification;
import com.trading.repository.BuyerNotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BuyerNotificationService {

    private final BuyerNotificationRepository buyerNotificationRepository;

    @Transactional(readOnly = true)
    public Page<BuyerNotificationResponse> listNotifications(Long buyerId, int page, int size) {
        return buyerNotificationRepository.findByBuyerIdOrderByCreatedAtDesc(buyerId, PageRequest.of(page, size))
                .map(BuyerNotificationResponse::from);
    }

    @Transactional(readOnly = true)
    public long countUnread(Long buyerId) {
        return buyerNotificationRepository.countByBuyerIdAndReadAtIsNull(buyerId);
    }

    @Transactional
    public BuyerNotificationResponse markRead(Long buyerId, Long notificationId) {
        BuyerNotification notification = buyerNotificationRepository.findByIdAndBuyerId(notificationId, buyerId)
                .orElseThrow(() -> BusinessException.notFound("通知不存在"));
        if (notification.getReadAt() == null) {
            notification.setReadAt(LocalDateTime.now());
            notification = buyerNotificationRepository.save(notification);
        }
        return BuyerNotificationResponse.from(notification);
    }

    @Transactional
    public long markAllRead(Long buyerId) {
        List<BuyerNotification> notifications = buyerNotificationRepository.findByBuyerIdAndReadAtIsNull(buyerId);
        LocalDateTime now = LocalDateTime.now();
        notifications.forEach(notification -> notification.setReadAt(now));
        buyerNotificationRepository.saveAll(notifications);
        return notifications.size();
    }
}
