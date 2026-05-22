package com.trading.repository;

import com.trading.entity.BuyerMembership;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BuyerMembershipRepository extends JpaRepository<BuyerMembership, Long> {
    Optional<BuyerMembership> findByBuyerId(Long buyerId);

    @Query("select bm from BuyerMembership bm left join fetch bm.plan where bm.buyerId = :buyerId")
    Optional<BuyerMembership> findByBuyerIdWithPlan(@Param("buyerId") Long buyerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select bm from BuyerMembership bm left join fetch bm.plan where bm.buyerId = :buyerId")
    Optional<BuyerMembership> findByBuyerIdForUpdate(@Param("buyerId") Long buyerId);
}
