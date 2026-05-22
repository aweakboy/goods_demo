package com.trading.repository;

import com.trading.entity.PriceAlert;
import com.trading.enums.PriceAlertStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface PriceAlertRepository extends JpaRepository<PriceAlert, Long> {
    Optional<PriceAlert> findByBuyerIdAndProductId(Long buyerId, Long productId);
    Page<PriceAlert> findByBuyerIdOrderByUpdatedAtDesc(Long buyerId, Pageable pageable);
    List<PriceAlert> findByProductIdAndStatusAndTargetPriceGreaterThanEqual(
            Long productId,
            PriceAlertStatus status,
            BigDecimal targetPrice
    );
}
