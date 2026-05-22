package com.trading.repository;

import com.trading.entity.MembershipPlan;
import com.trading.enums.MembershipPlanStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MembershipPlanRepository extends JpaRepository<MembershipPlan, Long> {

    List<MembershipPlan> findByStatusOrderByPriceAscIdDesc(MembershipPlanStatus status);

    @Query("select p from MembershipPlan p where (:status is null or p.status = :status)")
    Page<MembershipPlan> findByAdminFilter(@Param("status") MembershipPlanStatus status, Pageable pageable);
}
