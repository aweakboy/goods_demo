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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

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
        return prepareForOrder(buyerId, List.of(buyerCouponId), originalAmount).primaryUsage();
    }

    @Transactional
    public CouponUsagePlan prepareForOrder(Long buyerId, List<Long> buyerCouponIds, BigDecimal originalAmount) {
        if (buyerCouponIds == null || buyerCouponIds.isEmpty()) {
            return CouponUsagePlan.empty();
        }
        if (buyerCouponIds.size() > 2) {
            throw BusinessException.badRequest("最多只能选择2张优惠券");
        }
        if (buyerCouponIds.stream().anyMatch(Objects::isNull)) {
            throw BusinessException.badRequest("优惠券ID不正确");
        }
        Set<Long> uniqueIds = new HashSet<>(buyerCouponIds);
        if (uniqueIds.size() != buyerCouponIds.size()) {
            throw BusinessException.badRequest("不能重复使用同一张优惠券");
        }

        List<BuyerCoupon> buyerCoupons = new ArrayList<>();
        for (Long buyerCouponId : buyerCouponIds) {
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
            buyerCoupons.add(buyerCoupon);
        }

        if (buyerCoupons.size() == 2) {
            validateStackablePair(buyerCoupons);
        }

        buyerCoupons.sort((left, right) -> Integer.compare(
                audienceOrder(left.getCoupon().getAudience()),
                audienceOrder(right.getCoupon().getAudience())
        ));

        BigDecimal remainingAmount = originalAmount.max(BigDecimal.ZERO);
        List<CouponUsage> usages = new ArrayList<>();
        for (BuyerCoupon buyerCoupon : buyerCoupons) {
            Coupon coupon = buyerCoupon.getCoupon();
            BigDecimal appliedDiscount = coupon.getDiscountAmount().min(remainingAmount).max(BigDecimal.ZERO);
            remainingAmount = remainingAmount.subtract(appliedDiscount).max(BigDecimal.ZERO);
            usages.add(new CouponUsage(
                    buyerCoupon,
                    coupon.getId(),
                    buyerCoupon.getId(),
                    coupon.getName(),
                    coupon.getThresholdAmount(),
                    coupon.getDiscountAmount(),
                    appliedDiscount,
                    coupon.getAudience(),
                    Boolean.TRUE.equals(coupon.getStackable())
            ));
        }
        return new CouponUsagePlan(usages);
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

    public void markUsed(CouponUsagePlan plan, Long orderId) {
        if (plan == null) {
            return;
        }
        markUsed(plan.usages(), orderId);
    }

    public void markUsed(List<CouponUsage> usages, Long orderId) {
        if (usages == null || usages.isEmpty()) {
            return;
        }
        usages.forEach(usage -> markUsed(usage, orderId));
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

    @Transactional
    public void releaseForOrder(List<Long> buyerCouponIds, Long orderId) {
        if (buyerCouponIds == null || buyerCouponIds.isEmpty() || orderId == null) {
            return;
        }
        buyerCouponIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .forEach(buyerCouponId -> releaseForOrder(buyerCouponId, orderId));
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

    private void validateStackablePair(List<BuyerCoupon> buyerCoupons) {
        Set<CouponAudience> audiences = new HashSet<>();
        for (BuyerCoupon buyerCoupon : buyerCoupons) {
            Coupon coupon = buyerCoupon.getCoupon();
            if (!Boolean.TRUE.equals(coupon.getStackable())) {
                throw BusinessException.badRequest("所选优惠券不支持叠加使用");
            }
            audiences.add(coupon.getAudience());
        }
        if (!audiences.contains(CouponAudience.PUBLIC) || !audiences.contains(CouponAudience.MEMBER)) {
            throw BusinessException.badRequest("只能叠加一张普通券和一张会员专属券");
        }
    }

    private int audienceOrder(CouponAudience audience) {
        return audience == CouponAudience.MEMBER ? 1 : 0;
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
            BigDecimal appliedDiscount,
            CouponAudience audience,
            boolean stackable
    ) {
        public CouponUsage(
                BuyerCoupon buyerCoupon,
                Long couponId,
                Long buyerCouponId,
                String couponName,
                BigDecimal thresholdAmount,
                BigDecimal couponDiscountAmount,
                BigDecimal appliedDiscount
        ) {
            this(buyerCoupon, couponId, buyerCouponId, couponName, thresholdAmount, couponDiscountAmount,
                    appliedDiscount, null, false);
        }
    }

    public record CouponUsagePlan(List<CouponUsage> usages, BigDecimal totalDiscount) {
        public CouponUsagePlan {
            usages = usages == null ? List.of() : List.copyOf(usages);
            totalDiscount = totalDiscount == null ? BigDecimal.ZERO : totalDiscount;
        }

        public CouponUsagePlan(List<CouponUsage> usages) {
            this(usages, sum(usages));
        }

        public static CouponUsagePlan empty() {
            return new CouponUsagePlan(List.of(), BigDecimal.ZERO);
        }

        public CouponUsage primaryUsage() {
            return usages.isEmpty() ? null : usages.get(0);
        }

        private static BigDecimal sum(List<CouponUsage> usages) {
            if (usages == null || usages.isEmpty()) {
                return BigDecimal.ZERO;
            }
            return usages.stream()
                    .map(CouponUsage::appliedDiscount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
    }
}
