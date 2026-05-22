package com.trading.repository;

import com.trading.entity.MembershipPurchase;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MembershipPurchaseRepository extends JpaRepository<MembershipPurchase, Long> {
    List<MembershipPurchase> findByBuyerIdOrderByCreatedAtDesc(Long buyerId);
    Optional<MembershipPurchase> findByOutTradeNo(String outTradeNo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from MembershipPurchase p left join fetch p.plan where p.id = :id")
    Optional<MembershipPurchase> findByIdForUpdate(@Param("id") Long id);
}
