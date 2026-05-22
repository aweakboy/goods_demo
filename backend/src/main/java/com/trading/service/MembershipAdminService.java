package com.trading.service;

import com.trading.common.BusinessException;
import com.trading.dto.MembershipPlanRequest;
import com.trading.entity.Coupon;
import com.trading.entity.MembershipPlan;
import com.trading.enums.CouponAudience;
import com.trading.enums.MembershipPlanStatus;
import com.trading.repository.CouponRepository;
import com.trading.repository.MembershipPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class MembershipAdminService {

    private final MembershipPlanRepository membershipPlanRepository;
    private final CouponRepository couponRepository;

    public Page<MembershipPlan> list(MembershipPlanStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return membershipPlanRepository.findByAdminFilter(status, pageable);
    }

    @Transactional
    public MembershipPlan create(MembershipPlanRequest request) {
        validateRequest(request);
        MembershipPlan plan = MembershipPlan.builder()
                .name(trim(request.getName()))
                .description(trim(request.getDescription()))
                .price(request.getPrice())
                .durationMonths(request.getDurationMonths())
                .discountRate(request.getDiscountRate())
                .monthlyCouponId(request.getMonthlyCouponId())
                .status(request.getStatus() != null ? request.getStatus() : MembershipPlanStatus.ACTIVE)
                .build();
        return membershipPlanRepository.save(plan);
    }

    @Transactional
    public MembershipPlan update(Long planId, MembershipPlanRequest request) {
        MembershipPlan plan = membershipPlanRepository.findById(planId)
                .orElseThrow(() -> BusinessException.notFound("会员套餐不存在"));
        validateRequest(request);
        plan.setName(trim(request.getName()));
        plan.setDescription(trim(request.getDescription()));
        plan.setPrice(request.getPrice());
        plan.setDurationMonths(request.getDurationMonths());
        plan.setDiscountRate(request.getDiscountRate());
        plan.setMonthlyCouponId(request.getMonthlyCouponId());
        if (request.getStatus() != null) {
            plan.setStatus(request.getStatus());
        }
        return membershipPlanRepository.save(plan);
    }

    @Transactional
    public MembershipPlan activate(Long planId) {
        MembershipPlan plan = membershipPlanRepository.findById(planId)
                .orElseThrow(() -> BusinessException.notFound("会员套餐不存在"));
        plan.setStatus(MembershipPlanStatus.ACTIVE);
        return membershipPlanRepository.save(plan);
    }

    @Transactional
    public MembershipPlan deactivate(Long planId) {
        MembershipPlan plan = membershipPlanRepository.findById(planId)
                .orElseThrow(() -> BusinessException.notFound("会员套餐不存在"));
        plan.setStatus(MembershipPlanStatus.INACTIVE);
        return membershipPlanRepository.save(plan);
    }

    private void validateRequest(MembershipPlanRequest request) {
        if (request.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw BusinessException.badRequest("会员价格必须大于0");
        }
        if (request.getDurationMonths() == null || request.getDurationMonths() < 1) {
            throw BusinessException.badRequest("会员有效期至少1个月");
        }
        if (request.getDiscountRate().compareTo(BigDecimal.ZERO) <= 0
                || request.getDiscountRate().compareTo(BigDecimal.ONE) > 0) {
            throw BusinessException.badRequest("会员折扣率必须大于0且不能大于1");
        }
        if (request.getMonthlyCouponId() != null) {
            Coupon coupon = couponRepository.findById(request.getMonthlyCouponId())
                    .orElseThrow(() -> BusinessException.badRequest("会员专属优惠券不存在"));
            if (coupon.getAudience() != CouponAudience.MEMBER) {
                throw BusinessException.badRequest("每月专属优惠券必须配置为会员专属");
            }
        }
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
