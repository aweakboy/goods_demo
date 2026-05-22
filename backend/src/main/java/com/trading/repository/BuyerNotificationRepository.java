package com.trading.repository;

import com.trading.entity.BuyerNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BuyerNotificationRepository extends JpaRepository<BuyerNotification, Long> {
    Page<BuyerNotification> findByBuyerIdOrderByCreatedAtDesc(Long buyerId, Pageable pageable);
    long countByBuyerIdAndReadAtIsNull(Long buyerId);
    Optional<BuyerNotification> findByIdAndBuyerId(Long id, Long buyerId);
    List<BuyerNotification> findByBuyerIdAndReadAtIsNull(Long buyerId);
}
