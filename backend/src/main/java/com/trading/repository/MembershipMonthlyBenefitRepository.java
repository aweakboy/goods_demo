package com.trading.repository;

import com.trading.entity.MembershipMonthlyBenefit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MembershipMonthlyBenefitRepository extends JpaRepository<MembershipMonthlyBenefit, Long> {
    Optional<MembershipMonthlyBenefit> findByBuyerIdAndPlanIdAndBenefitMonth(Long buyerId, Long planId, String benefitMonth);
    boolean existsByBuyerIdAndPlanIdAndBenefitMonth(Long buyerId, Long planId, String benefitMonth);
}
