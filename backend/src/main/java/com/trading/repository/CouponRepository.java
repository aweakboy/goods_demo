package com.trading.repository;

import com.trading.entity.Coupon;
import com.trading.enums.CouponAudience;
import com.trading.enums.CouponStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Long> {

    @Query("select c from Coupon c where (:status is null or c.status = :status)")
    Page<Coupon> findByAdminFilter(@Param("status") CouponStatus status, Pageable pageable);

    @Query("select c from Coupon c " +
            "where c.status = :status and c.audience = :audience and c.validFrom <= :now and c.validTo >= :now " +
            "and c.claimedQuantity < c.totalQuantity " +
            "order by c.validTo asc, c.id desc")
    List<Coupon> findClaimable(@Param("status") CouponStatus status,
                               @Param("audience") CouponAudience audience,
                               @Param("now") LocalDateTime now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Coupon c where c.id = :id")
    Optional<Coupon> findByIdForUpdate(@Param("id") Long id);
}
