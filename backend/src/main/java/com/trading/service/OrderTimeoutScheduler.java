package com.trading.service;

import com.trading.entity.Order;
import com.trading.enums.OrderStatus;
import com.trading.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutScheduler {

    private final OrderRepository orderRepository;
    private final OrderService orderService;

    @Scheduled(fixedDelay = 60000)
    public void cancelExpiredOrders() {
        List<Order> expired = orderRepository.findByStatusAndExpiredAtBefore(
                OrderStatus.PENDING_PAYMENT, LocalDateTime.now());
        if (expired.isEmpty()) return;
        log.info("Cancelling {} expired orders", expired.size());
        expired.forEach(order -> {
            try {
                orderService.doCancelOrder(order.getId());
            } catch (Exception e) {
                log.warn("Failed to cancel expired order {}: {}", order.getId(), e.getMessage());
            }
        });
    }
}
