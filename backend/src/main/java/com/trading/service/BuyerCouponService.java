package com.trading.service;

import com.trading.common.BusinessException;
import com.trading.entity.BuyerCoupon;
import com.trading.entity.Coupon;
import com.trading.enums.BuyerCouponStatus;
import com.trading.enums.CouponAudience;
import com.trading.enums.CouponStatus;
import com.trading.repository.BuyerCouponRepository;
import com.trading.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BuyerCouponService {

    private final CouponRepository couponRepository;
    private final BuyerCouponRepository buyerCouponRepository;

    public List<Coupon> listClaimableCoupons() {
        return couponRepository.findClaimable(CouponStatus.ACTIVE, CouponAudience.PUBLIC, LocalDateTime.now());
    }

    public long countClaimedByBuyer(Long buyerId, Long couponId) {
        return buyerCouponRepository.countByBuyerIdAndCouponId(buyerId, couponId);
    }

    @Transactional
    public BuyerCoupon claim(Long buyerId, Long couponId) {
        Coupon coupon = couponRepository.findByIdForUpdate(couponId)
                .orElseThrow(() -> BusinessException.notFound("优惠券不存在"));
        validateClaimable(buyerId, coupon);

        coupon.setClaimedQuantity(coupon.getClaimedQuantity() + 1);
        couponRepository.save(coupon);
        BuyerCoupon buyerCoupon = BuyerCoupon.builder()
                .buyerId(buyerId)
                .couponId(coupon.getId())
                .status(BuyerCouponStatus.UNUSED)
                .build();
        buyerCoupon = buyerCouponRepository.save(buyerCoupon);
        buyerCoupon.setCoupon(coupon);
        return buyerCoupon;
    }

    @Transactional
    public BuyerCoupon claimMembershipCoupon(Long buyerId, Long couponId) {
        Coupon coupon = couponRepository.findByIdForUpdate(couponId)
                .orElseThrow(() -> BusinessException.notFound("优惠券不存在"));
        validateMembershipClaimable(coupon);

        coupon.setClaimedQuantity(coupon.getClaimedQuantity() + 1);
        couponRepository.save(coupon);
        BuyerCoupon buyerCoupon = BuyerCoupon.builder()
                .buyerId(buyerId)
                .couponId(coupon.getId())
                .status(BuyerCouponStatus.UNUSED)
                .build();
        buyerCoupon = buyerCouponRepository.save(buyerCoupon);
        buyerCoupon.setCoupon(coupon);
        return buyerCoupon;
    }

    public List<BuyerCoupon> listMine(Long buyerId, BuyerCouponStatus status) {
        List<BuyerCoupon> coupons = buyerCouponRepository.findByBuyerIdWithCoupon(buyerId);
        if (status == null) {
            return coupons;
        }
        LocalDateTime now = LocalDateTime.now();
        return coupons.stream()
                .filter(buyerCoupon -> displayStatus(buyerCoupon, now) == status)
                .toList();
    }

    public List<BuyerCoupon> listUsable(Long buyerId) {
        LocalDateTime now = LocalDateTime.now();
        return buyerCouponRepository.findByBuyerIdAndStatusWithCoupon(buyerId, BuyerCouponStatus.UNUSED).stream()
                .filter(buyerCoupon -> displayStatus(buyerCoupon, now) == BuyerCouponStatus.UNUSED)
                .filter(buyerCoupon -> buyerCoupon.getCoupon() != null && buyerCoupon.getCoupon().getStatus() == CouponStatus.ACTIVE)
                .toList();
    }

    @Transactional
    public CouponUsage prepareForOrder(Long buyerId, Long buyerCouponId, BigDecimal originalAmount) {
        BuyerCoupon buyerCoupon = buyerCouponRepository.findByIdForUpdate(buyerCouponId)
                .orElseThrow(() -> BusinessException.notFound("优惠券不存在"));
        if (!buyerCoupon.getBuyerId().equals(buyerId)) {
            throw BusinessException.forbidden("无权使用该优惠券");
        }
        Coupon coupon = buyerCoupon.getCoupon();
        if (coupon == null) {
            throw BusinessException.notFound("优惠券不存在");
        }
        validateUsableForOrder(buyerCoupon, coupon, originalAmount);
        BigDecimal discount = coupon.getDiscountAmount().min(originalAmount);
        return new CouponUsage(
                buyerCoupon,
                coupon.getId(),
                buyerCoupon.getId(),
                coupon.getName(),
                coupon.getThresholdAmount(),
                coupon.getDiscountAmount(),
                discount
        );
    }

    public void markUsed(CouponUsage usage, Long orderId) {
        if (usage == null) {
            return;
        }
        BuyerCoupon buyerCoupon = usage.buyerCoupon();
        buyerCoupon.setStatus(BuyerCouponStatus.USED);
        buyerCoupon.setUsedAt(LocalDateTime.now());
        buyerCoupon.setUsedOrderId(orderId);
        buyerCouponRepository.save(buyerCoupon);
    }

    @Transactional
    public void releaseForOrder(Long buyerCouponId, Long orderId) {
        if (buyerCouponId == null || orderId == null) {
            return;
        }
        buyerCouponRepository.findByIdForUpdate(buyerCouponId).ifPresent(buyerCoupon -> {
            if (buyerCoupon.getStatus() != BuyerCouponStatus.USED) {
                return;
            }
            if (!orderId.equals(buyerCoupon.getUsedOrderId())) {
                return;
            }
            buyerCoupon.setStatus(BuyerCouponStatus.UNUSED);
            buyerCoupon.setUsedAt(null);
            buyerCoupon.setUsedOrderId(null);
            buyerCouponRepository.save(buyerCoupon);
        });
    }

    public BuyerCouponStatus displayStatus(BuyerCoupon buyerCoupon) {
        return displayStatus(buyerCoupon, LocalDateTime.now());
    }

    private void validateClaimable(Long buyerId, Coupon coupon) {
        LocalDateTime now = LocalDateTime.now();
        if (coupon.getStatus() != CouponStatus.ACTIVE) {
            throw BusinessException.badRequest("优惠券已停用");
        }
        if (coupon.getAudience() != CouponAudience.PUBLIC) {
            throw BusinessException.badRequest("该优惠券仅限会员权益领取");
        }
        if (now.isBefore(coupon.getValidFrom())) {
            throw BusinessException.badRequest("优惠券尚未开始");
        }
        if (now.isAfter(coupon.getValidTo())) {
            throw BusinessException.badRequest("优惠券已过期");
        }
        if (coupon.getClaimedQuantity() >= coupon.getTotalQuantity()) {
            throw BusinessException.badRequest("优惠券已领完");
        }
        long claimedByBuyer = buyerCouponRepository.countByBuyerIdAndCouponId(buyerId, coupon.getId());
        if (claimedByBuyer >= coupon.getPerUserLimit()) {
            throw BusinessException.badRequest("已达到该优惠券领取上限");
        }
    }

    private void validateMembershipClaimable(Coupon coupon) {
        LocalDateTime now = LocalDateTime.now();
        if (coupon.getAudience() != CouponAudience.MEMBER) {
            throw BusinessException.badRequest("该优惠券不是会员专属券");
        }
        if (coupon.getStatus() != CouponStatus.ACTIVE) {
            throw BusinessException.badRequest("会员专属券已停用");
        }
        if (now.isBefore(coupon.getValidFrom())) {
            throw BusinessException.badRequest("会员专属券尚未开始");
        }
        if (now.isAfter(coupon.getValidTo())) {
            throw BusinessException.badRequest("会员专属券已过期");
        }
        if (coupon.getClaimedQuantity() >= coupon.getTotalQuantity()) {
            throw BusinessException.badRequest("会员专属券已领完");
        }
    }

    private void validateUsableForOrder(BuyerCoupon buyerCoupon, Coupon coupon, BigDecimal originalAmount) {
        if (buyerCoupon.getStatus() != BuyerCouponStatus.UNUSED) {
            throw BusinessException.badRequest("优惠券不可用");
        }
        if (coupon.getStatus() != CouponStatus.ACTIVE) {
            throw BusinessException.badRequest("优惠券已停用");
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(coupon.getValidFrom())) {
            throw BusinessException.badRequest("优惠券尚未开始");
        }
        if (now.isAfter(coupon.getValidTo())) {
            throw BusinessException.badRequest("优惠券已过期");
        }
        if (originalAmount.compareTo(coupon.getThresholdAmount()) < 0) {
            throw BusinessException.badRequest("订单金额未达到优惠券使用门槛");
        }
    }

    private BuyerCouponStatus displayStatus(BuyerCoupon buyerCoupon, LocalDateTime now) {
        Coupon coupon = buyerCoupon.getCoupon();
        if (buyerCoupon.getStatus() == BuyerCouponStatus.UNUSED
                && coupon != null
                && coupon.getValidTo() != null
                && now.isAfter(coupon.getValidTo())) {
            return BuyerCouponStatus.EXPIRED;
        }
        return buyerCoupon.getStatus();
    }

    public record CouponUsage(
            BuyerCoupon buyerCoupon,
            Long couponId,
            Long buyerCouponId,
            String couponName,
            BigDecimal thresholdAmount,
            BigDecimal couponDiscountAmount,
            BigDecimal appliedDiscount
    ) {}
}
