package com.trading.service;

import com.trading.common.BusinessException;
import com.trading.entity.*;
import com.trading.enums.BuyerMembershipStatus;
import com.trading.enums.MembershipPlanStatus;
import com.trading.enums.MembershipPurchaseStatus;
import com.trading.repository.BuyerMembershipRepository;
import com.trading.repository.MembershipMonthlyBenefitRepository;
import com.trading.repository.MembershipPlanRepository;
import com.trading.repository.MembershipPurchaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MembershipService {

    private final MembershipPlanRepository membershipPlanRepository;
    private final BuyerMembershipRepository buyerMembershipRepository;
    private final MembershipPurchaseRepository membershipPurchaseRepository;
    private final MembershipMonthlyBenefitRepository membershipMonthlyBenefitRepository;
    private final BuyerCouponService buyerCouponService;

    public List<MembershipPlan> listActivePlans() {
        return membershipPlanRepository.findByStatusOrderByPriceAscIdDesc(MembershipPlanStatus.ACTIVE);
    }

    public List<MembershipPurchase> listPurchases(Long buyerId) {
        return membershipPurchaseRepository.findByBuyerIdOrderByCreatedAtDesc(buyerId);
    }

    @Transactional
    public MembershipPurchase createPurchase(Long buyerId, Long planId) {
        MembershipPlan plan = membershipPlanRepository.findById(planId)
                .orElseThrow(() -> BusinessException.notFound("会员套餐不存在"));
        if (plan.getStatus() != MembershipPlanStatus.ACTIVE) {
            throw BusinessException.badRequest("会员套餐已停用");
        }
        MembershipPurchase purchase = MembershipPurchase.builder()
                .buyerId(buyerId)
                .planId(plan.getId())
                .amount(plan.getPrice())
                .status(MembershipPurchaseStatus.PENDING_PAYMENT)
                .build();
        purchase = membershipPurchaseRepository.save(purchase);
        purchase.setOutTradeNo(outTradeNo(purchase.getId()));
        purchase.setPlan(plan);
        return membershipPurchaseRepository.save(purchase);
    }

    @Transactional
    public MembershipPurchase handlePaymentSuccess(Long purchaseId, String alipayTradeNo) {
        return handlePaymentSuccess(purchaseId, alipayTradeNo, LocalDateTime.now());
    }

    @Transactional
    public MembershipPurchase handlePaymentSuccess(Long purchaseId, String alipayTradeNo, LocalDateTime paidAt) {
        MembershipPurchase purchase = membershipPurchaseRepository.findByIdForUpdate(purchaseId)
                .orElseThrow(() -> BusinessException.notFound("会员购买记录不存在"));
        if (purchase.getStatus() == MembershipPurchaseStatus.PAID) {
            if (purchase.getAlipayTradeNo() == null && alipayTradeNo != null) {
                purchase.setAlipayTradeNo(alipayTradeNo);
                return membershipPurchaseRepository.save(purchase);
            }
            return purchase;
        }
        if (purchase.getStatus() != MembershipPurchaseStatus.PENDING_PAYMENT) {
            throw BusinessException.badRequest("会员购买记录状态不允许支付");
        }
        MembershipPlan plan = purchase.getPlan();
        if (plan == null) {
            plan = membershipPlanRepository.findById(purchase.getPlanId())
                    .orElseThrow(() -> BusinessException.notFound("会员套餐不存在"));
        }

        BuyerMembership membership = buyerMembershipRepository.findByBuyerIdForUpdate(purchase.getBuyerId())
                .orElse(null);
        LocalDateTime baseTime = paidAt;
        if (membership != null && membership.getExpiresAt() != null && membership.getExpiresAt().isAfter(paidAt)) {
            baseTime = membership.getExpiresAt();
        }
        LocalDateTime expiresAt = baseTime.plusMonths(plan.getDurationMonths());

        if (membership == null) {
            membership = BuyerMembership.builder()
                    .buyerId(purchase.getBuyerId())
                    .planId(plan.getId())
                    .status(BuyerMembershipStatus.ACTIVE)
                    .startedAt(paidAt)
                    .expiresAt(expiresAt)
                    .lastPaidAt(paidAt)
                    .build();
        } else {
            if (membership.getExpiresAt() == null || !membership.getExpiresAt().isAfter(paidAt)) {
                membership.setStartedAt(paidAt);
            }
            membership.setPlanId(plan.getId());
            membership.setStatus(BuyerMembershipStatus.ACTIVE);
            membership.setExpiresAt(expiresAt);
            membership.setLastPaidAt(paidAt);
        }
        buyerMembershipRepository.save(membership);

        purchase.setStatus(MembershipPurchaseStatus.PAID);
        purchase.setAlipayTradeNo(alipayTradeNo);
        purchase.setPaidAt(paidAt);
        return membershipPurchaseRepository.save(purchase);
    }

    public Optional<BuyerMembership> findActiveMembership(Long buyerId) {
        LocalDateTime now = LocalDateTime.now();
        return buyerMembershipRepository.findByBuyerIdWithPlan(buyerId)
                .filter(membership -> displayStatus(membership, now) == BuyerMembershipStatus.ACTIVE)
                .filter(membership -> membership.getPlan() != null);
    }

    public BuyerMembershipStatus displayStatus(BuyerMembership membership) {
        return displayStatus(membership, LocalDateTime.now());
    }

    public BuyerMembershipStatus displayStatus(BuyerMembership membership, LocalDateTime now) {
        if (membership.getExpiresAt() != null && membership.getExpiresAt().isBefore(now)) {
            return BuyerMembershipStatus.EXPIRED;
        }
        return membership.getStatus();
    }

    public MembershipStatus status(Long buyerId) {
        LocalDateTime now = LocalDateTime.now();
        Optional<BuyerMembership> membershipOpt = buyerMembershipRepository.findByBuyerIdWithPlan(buyerId);
        if (membershipOpt.isEmpty()) {
            return MembershipStatus.none();
        }
        BuyerMembership membership = membershipOpt.get();
        BuyerMembershipStatus displayStatus = displayStatus(membership, now);
        boolean claimed = false;
        if (displayStatus == BuyerMembershipStatus.ACTIVE) {
            claimed = membershipMonthlyBenefitRepository.existsByBuyerIdAndPlanIdAndBenefitMonth(
                    buyerId, membership.getPlanId(), currentBenefitMonth());
        }
        return new MembershipStatus(membership, displayStatus, claimed);
    }

    @Transactional
    public MembershipMonthlyBenefit claimMonthlyBenefit(Long buyerId) {
        BuyerMembership membership = buyerMembershipRepository.findByBuyerIdWithPlan(buyerId)
                .orElseThrow(() -> BusinessException.badRequest("当前不是会员"));
        if (displayStatus(membership) != BuyerMembershipStatus.ACTIVE) {
            throw BusinessException.badRequest("会员已过期");
        }
        MembershipPlan plan = membership.getPlan();
        if (plan == null) {
            plan = membershipPlanRepository.findById(membership.getPlanId())
                    .orElseThrow(() -> BusinessException.notFound("会员套餐不存在"));
        }
        if (plan.getMonthlyCouponId() == null) {
            throw BusinessException.badRequest("当前套餐暂无可领取专属券");
        }
        String month = currentBenefitMonth();
        if (membershipMonthlyBenefitRepository.existsByBuyerIdAndPlanIdAndBenefitMonth(buyerId, plan.getId(), month)) {
            throw BusinessException.badRequest("本月会员权益已领取");
        }

        BuyerCoupon buyerCoupon = buyerCouponService.claimMembershipCoupon(buyerId, plan.getMonthlyCouponId());
        MembershipMonthlyBenefit benefit = MembershipMonthlyBenefit.builder()
                .buyerId(buyerId)
                .planId(plan.getId())
                .benefitMonth(month)
                .couponId(plan.getMonthlyCouponId())
                .buyerCouponId(buyerCoupon.getId())
                .claimedAt(LocalDateTime.now())
                .build();
        return membershipMonthlyBenefitRepository.save(benefit);
    }

    public OrderMembershipDiscount prepareOrderDiscount(Long buyerId, BigDecimal amountAfterCoupon) {
        if (amountAfterCoupon == null || amountAfterCoupon.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return findActiveMembership(buyerId)
                .map(BuyerMembership::getPlan)
                .filter(plan -> plan.getDiscountRate() != null)
                .map(plan -> {
                    BigDecimal discountRate = plan.getDiscountRate();
                    BigDecimal discountAmount = amountAfterCoupon
                            .multiply(BigDecimal.ONE.subtract(discountRate))
                            .setScale(2, RoundingMode.HALF_UP);
                    if (discountAmount.compareTo(amountAfterCoupon) > 0) {
                        discountAmount = amountAfterCoupon;
                    }
                    return new OrderMembershipDiscount(
                            plan.getId(),
                            plan.getName(),
                            discountRate,
                            discountAmount.max(BigDecimal.ZERO)
                    );
                })
                .orElse(null);
    }

    public static String outTradeNo(Long purchaseId) {
        return "MEM-" + purchaseId;
    }

    public static Long parsePurchaseId(String outTradeNo) {
        if (outTradeNo == null || !outTradeNo.startsWith("MEM-")) {
            throw BusinessException.badRequest("会员支付订单号无效");
        }
        try {
            return Long.parseLong(outTradeNo.substring(4));
        } catch (RuntimeException e) {
            throw BusinessException.badRequest("会员支付订单号无效");
        }
    }

    public static String currentBenefitMonth() {
        return YearMonth.now().toString();
    }

    public record MembershipStatus(
            BuyerMembership membership,
            BuyerMembershipStatus displayStatus,
            boolean currentMonthBenefitClaimed
    ) {
        public static MembershipStatus none() {
            return new MembershipStatus(null, null, false);
        }
    }

    public record OrderMembershipDiscount(
            Long planId,
            String planName,
            BigDecimal discountRate,
            BigDecimal discountAmount
    ) {}
}
