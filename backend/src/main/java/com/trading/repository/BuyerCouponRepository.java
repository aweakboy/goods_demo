package com.trading.repository;

import com.trading.entity.BuyerCoupon;
import com.trading.enums.BuyerCouponStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BuyerCouponRepository extends JpaRepository<BuyerCoupon, Long> {
    List<BuyerCoupon> findByBuyerIdOrderByClaimedAtDescIdDesc(Long buyerId);
    List<BuyerCoupon> findByBuyerIdAndStatusOrderByClaimedAtDescIdDesc(Long buyerId, BuyerCouponStatus status);
    Optional<BuyerCoupon> findByIdAndBuyerId(Long id, Long buyerId);
    long countByBuyerIdAndCouponId(Long buyerId, Long couponId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select bc from BuyerCoupon bc join fetch bc.coupon where bc.id = :id")
    Optional<BuyerCoupon> findByIdForUpdate(@Param("id") Long id);

    @Query("select bc from BuyerCoupon bc join fetch bc.coupon where bc.buyerId = :buyerId order by bc.claimedAt desc, bc.id desc")
    List<BuyerCoupon> findByBuyerIdWithCoupon(@Param("buyerId") Long buyerId);

    @Query("select bc from BuyerCoupon bc join fetch bc.coupon where bc.buyerId = :buyerId and bc.status = :status order by bc.claimedAt desc, bc.id desc")
    List<BuyerCoupon> findByBuyerIdAndStatusWithCoupon(@Param("buyerId") Long buyerId, @Param("status") BuyerCouponStatus status);
}
