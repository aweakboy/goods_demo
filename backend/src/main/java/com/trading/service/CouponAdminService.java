package com.trading.service;

import com.trading.common.BusinessException;
import com.trading.dto.CouponRequest;
import com.trading.entity.Coupon;
import com.trading.enums.CouponAudience;
import com.trading.enums.CouponStatus;
import com.trading.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CouponAdminService {

    private final CouponRepository couponRepository;

    public Page<Coupon> list(CouponStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return couponRepository.findByAdminFilter(status, pageable);
    }

    @Transactional
    public Coupon create(Long adminId, CouponRequest request) {
        validateRequest(request, false, null);
        Coupon coupon = Coupon.builder()
                .name(trim(request.getName()))
                .description(trim(request.getDescription()))
                .thresholdAmount(request.getThresholdAmount())
                .discountAmount(request.getDiscountAmount())
                .totalQuantity(request.getTotalQuantity())
                .claimedQuantity(0)
                .perUserLimit(request.getPerUserLimit())
                .validFrom(request.getValidFrom())
                .validTo(request.getValidTo())
                .status(request.getStatus() != null ? request.getStatus() : CouponStatus.ACTIVE)
                .audience(request.getAudience() != null ? request.getAudience() : CouponAudience.PUBLIC)
                .createdBy(adminId)
                .build();
        return couponRepository.save(coupon);
    }

    @Transactional
    public Coupon update(Long couponId, CouponRequest request) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> BusinessException.notFound("优惠券不存在"));
        validateRequest(request, true, coupon);
        coupon.setName(trim(request.getName()));
        coupon.setDescription(trim(request.getDescription()));
        coupon.setThresholdAmount(request.getThresholdAmount());
        coupon.setDiscountAmount(request.getDiscountAmount());
        coupon.setTotalQuantity(request.getTotalQuantity());
        coupon.setPerUserLimit(request.getPerUserLimit());
        coupon.setValidFrom(request.getValidFrom());
        coupon.setValidTo(request.getValidTo());
        if (request.getStatus() != null) {
            coupon.setStatus(request.getStatus());
        }
        coupon.setAudience(request.getAudience() != null ? request.getAudience() : CouponAudience.PUBLIC);
        return couponRepository.save(coupon);
    }

    @Transactional
    public Coupon activate(Long couponId) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> BusinessException.notFound("优惠券不存在"));
        coupon.setStatus(CouponStatus.ACTIVE);
        return couponRepository.save(coupon);
    }

    @Transactional
    public Coupon deactivate(Long couponId) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> BusinessException.notFound("优惠券不存在"));
        coupon.setStatus(CouponStatus.INACTIVE);
        return couponRepository.save(coupon);
    }

    private void validateRequest(CouponRequest request, boolean update, Coupon existing) {
        if (request.getDiscountAmount().compareTo(request.getThresholdAmount()) > 0) {
            throw BusinessException.badRequest("抵扣金额不能大于满减门槛");
        }
        if (!request.getValidTo().isAfter(request.getValidFrom())) {
            throw BusinessException.badRequest("有效期结束时间必须晚于开始时间");
        }
        if (update && existing != null && request.getTotalQuantity() < existing.getClaimedQuantity()) {
            throw BusinessException.badRequest("发放总量不能小于已领取数量");
        }
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
