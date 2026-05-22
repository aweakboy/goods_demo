package com.trading.service;

import com.trading.common.BusinessException;
import com.trading.entity.*;
import com.trading.enums.*;
import com.trading.repository.BuyerMembershipRepository;
import com.trading.repository.MembershipMonthlyBenefitRepository;
import com.trading.repository.MembershipPlanRepository;
import com.trading.repository.MembershipPurchaseRepository;
import jakarta.persistence.Table;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MembershipServiceTest {

    @Mock MembershipPlanRepository membershipPlanRepository;
    @Mock BuyerMembershipRepository buyerMembershipRepository;
    @Mock MembershipPurchaseRepository membershipPurchaseRepository;
    @Mock MembershipMonthlyBenefitRepository membershipMonthlyBenefitRepository;
    @Mock BuyerCouponService buyerCouponService;

    @InjectMocks MembershipService membershipService;

    @Test
    void createPurchase_activePlan_createsPendingPurchaseWithOutTradeNo() {
        MembershipPlan plan = plan();
        when(membershipPlanRepository.findById(1L)).thenReturn(Optional.of(plan));
        when(membershipPurchaseRepository.findFirstByBuyerIdAndPlanIdAndStatusOrderByCreatedAtDesc(
                2L, 1L, MembershipPurchaseStatus.PENDING_PAYMENT)).thenReturn(Optional.empty());
        when(membershipPurchaseRepository.save(any(MembershipPurchase.class))).thenAnswer(i -> {
            MembershipPurchase purchase = i.getArgument(0);
            if (purchase.getId() == null) {
                purchase.setId(20L);
            }
            return purchase;
        });

        MembershipPurchase purchase = membershipService.createPurchase(2L, 1L);

        assertEquals(MembershipPurchaseStatus.PENDING_PAYMENT, purchase.getStatus());
        assertEquals("MEM-20", purchase.getOutTradeNo());
        assertEquals(BigDecimal.valueOf(30), purchase.getAmount());
    }

    @Test
    void createPurchase_existingPendingPurchase_reusesIt() {
        MembershipPlan plan = plan();
        MembershipPurchase existing = pendingPurchase();
        when(membershipPlanRepository.findById(1L)).thenReturn(Optional.of(plan));
        when(membershipPurchaseRepository.findFirstByBuyerIdAndPlanIdAndStatusOrderByCreatedAtDesc(
                2L, 1L, MembershipPurchaseStatus.PENDING_PAYMENT)).thenReturn(Optional.of(existing));

        MembershipPurchase purchase = membershipService.createPurchase(2L, 1L);

        assertSame(existing, purchase);
        assertEquals("MEM-20", purchase.getOutTradeNo());
        assertSame(plan, purchase.getPlan());
        verify(membershipPurchaseRepository, never()).save(any());
    }

    @Test
    void createPurchase_existingPendingPurchaseWithoutOutTradeNo_repairsAndReusesIt() {
        MembershipPlan plan = plan();
        MembershipPurchase existing = pendingPurchase();
        existing.setOutTradeNo(null);
        when(membershipPlanRepository.findById(1L)).thenReturn(Optional.of(plan));
        when(membershipPurchaseRepository.findFirstByBuyerIdAndPlanIdAndStatusOrderByCreatedAtDesc(
                2L, 1L, MembershipPurchaseStatus.PENDING_PAYMENT)).thenReturn(Optional.of(existing));
        when(membershipPurchaseRepository.save(existing)).thenReturn(existing);

        MembershipPurchase purchase = membershipService.createPurchase(2L, 1L);

        assertSame(existing, purchase);
        assertEquals("MEM-20", purchase.getOutTradeNo());
        verify(membershipPurchaseRepository).save(existing);
    }

    @Test
    void createPurchase_inactivePlan_throwsBadRequest() {
        MembershipPlan plan = plan();
        plan.setStatus(MembershipPlanStatus.INACTIVE);
        when(membershipPlanRepository.findById(1L)).thenReturn(Optional.of(plan));

        BusinessException ex = assertThrows(BusinessException.class, () -> membershipService.createPurchase(2L, 1L));

        assertEquals(400, ex.getStatus());
        verify(membershipPurchaseRepository, never()).save(any());
    }

    @Test
    void preparePendingPurchasePayment_ownPendingPurchase_returnsPayablePurchase() {
        MembershipPurchase purchase = pendingPurchase();
        purchase.setPlan(plan());
        when(membershipPurchaseRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(purchase));

        MembershipPurchase result = membershipService.preparePendingPurchasePayment(2L, 20L);

        assertSame(purchase, result);
        assertEquals("MEM-20", result.getOutTradeNo());
        verify(membershipPurchaseRepository, never()).save(any());
    }

    @Test
    void preparePendingPurchasePayment_missingOutTradeNo_repairsBeforeReturn() {
        MembershipPurchase purchase = pendingPurchase();
        purchase.setOutTradeNo(null);
        purchase.setPlan(plan());
        when(membershipPurchaseRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(purchase));
        when(membershipPurchaseRepository.save(purchase)).thenReturn(purchase);

        MembershipPurchase result = membershipService.preparePendingPurchasePayment(2L, 20L);

        assertEquals("MEM-20", result.getOutTradeNo());
        verify(membershipPurchaseRepository).save(purchase);
    }

    @Test
    void preparePendingPurchasePayment_nonPendingPurchase_throwsBadRequest() {
        MembershipPurchase purchase = pendingPurchase();
        purchase.setStatus(MembershipPurchaseStatus.PAID);
        when(membershipPurchaseRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(purchase));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> membershipService.preparePendingPurchasePayment(2L, 20L));

        assertEquals(400, ex.getStatus());
        verify(membershipPurchaseRepository, never()).save(any());
    }

    @Test
    void preparePendingPurchasePayment_foreignPurchase_throwsForbidden() {
        MembershipPurchase purchase = pendingPurchase();
        purchase.setBuyerId(3L);
        when(membershipPurchaseRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(purchase));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> membershipService.preparePendingPurchasePayment(2L, 20L));

        assertEquals(403, ex.getStatus());
        verify(membershipPurchaseRepository, never()).save(any());
    }

    @Test
    void handlePaymentSuccess_firstPurchase_createsActiveMembership() {
        LocalDateTime paidAt = LocalDateTime.now();
        MembershipPurchase purchase = pendingPurchase();
        purchase.setPlan(plan());
        when(membershipPurchaseRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(purchase));
        when(buyerMembershipRepository.findByBuyerIdForUpdate(2L)).thenReturn(Optional.empty());
        when(membershipPurchaseRepository.save(any(MembershipPurchase.class))).thenAnswer(i -> i.getArgument(0));
        when(buyerMembershipRepository.save(any(BuyerMembership.class))).thenAnswer(i -> i.getArgument(0));

        MembershipPurchase result = membershipService.handlePaymentSuccess(20L, "trade-20", paidAt);

        assertEquals(MembershipPurchaseStatus.PAID, result.getStatus());
        assertEquals("trade-20", result.getAlipayTradeNo());
        verify(buyerMembershipRepository).save(argThat(membership ->
                membership.getBuyerId().equals(2L)
                        && membership.getStatus() == BuyerMembershipStatus.ACTIVE
                        && membership.getExpiresAt().equals(paidAt.plusMonths(1))));
    }

    @Test
    void handlePaymentSuccess_activeMembership_renewsFromCurrentExpiry() {
        LocalDateTime paidAt = LocalDateTime.now();
        BuyerMembership membership = activeMembership(paidAt.plusDays(10));
        MembershipPurchase purchase = pendingPurchase();
        purchase.setPlan(plan());
        when(membershipPurchaseRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(purchase));
        when(buyerMembershipRepository.findByBuyerIdForUpdate(2L)).thenReturn(Optional.of(membership));
        when(membershipPurchaseRepository.save(any(MembershipPurchase.class))).thenAnswer(i -> i.getArgument(0));

        membershipService.handlePaymentSuccess(20L, "trade-20", paidAt);

        assertEquals(paidAt.plusDays(10).plusMonths(1), membership.getExpiresAt());
        assertEquals(paidAt, membership.getLastPaidAt());
    }

    @Test
    void handlePaymentSuccess_expiredMembership_restartsFromPaidAt() {
        LocalDateTime paidAt = LocalDateTime.now();
        BuyerMembership membership = activeMembership(paidAt.minusDays(1));
        MembershipPurchase purchase = pendingPurchase();
        purchase.setPlan(plan());
        when(membershipPurchaseRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(purchase));
        when(buyerMembershipRepository.findByBuyerIdForUpdate(2L)).thenReturn(Optional.of(membership));
        when(membershipPurchaseRepository.save(any(MembershipPurchase.class))).thenAnswer(i -> i.getArgument(0));

        membershipService.handlePaymentSuccess(20L, "trade-20", paidAt);

        assertEquals(paidAt, membership.getStartedAt());
        assertEquals(paidAt.plusMonths(1), membership.getExpiresAt());
    }

    @Test
    void handlePaymentSuccess_paidPurchase_isIdempotent() {
        MembershipPurchase purchase = pendingPurchase();
        purchase.setStatus(MembershipPurchaseStatus.PAID);
        purchase.setAlipayTradeNo("trade-20");
        when(membershipPurchaseRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(purchase));

        MembershipPurchase result = membershipService.handlePaymentSuccess(20L, "trade-20", LocalDateTime.now());

        assertSame(purchase, result);
        verify(buyerMembershipRepository, never()).save(any());
        verify(membershipPurchaseRepository, never()).save(any());
    }

    @Test
    void claimMonthlyBenefit_activeMember_createsBuyerCouponAndBenefitRecord() {
        BuyerMembership membership = activeMembership(LocalDateTime.now().plusDays(20));
        membership.setPlan(plan());
        BuyerCoupon buyerCoupon = BuyerCoupon.builder().id(30L).buyerId(2L).couponId(10L).status(BuyerCouponStatus.UNUSED).build();
        when(buyerMembershipRepository.findByBuyerIdWithPlan(2L)).thenReturn(Optional.of(membership));
        when(membershipMonthlyBenefitRepository.existsByBuyerIdAndPlanIdAndBenefitMonth(eq(2L), eq(1L), anyString())).thenReturn(false);
        when(buyerCouponService.claimMembershipCoupon(2L, 10L)).thenReturn(buyerCoupon);
        when(membershipMonthlyBenefitRepository.save(any(MembershipMonthlyBenefit.class))).thenAnswer(i -> {
            MembershipMonthlyBenefit benefit = i.getArgument(0);
            benefit.setId(40L);
            return benefit;
        });

        MembershipMonthlyBenefit benefit = membershipService.claimMonthlyBenefit(2L);

        assertEquals(40L, benefit.getId());
        assertEquals(30L, benefit.getBuyerCouponId());
        assertEquals(10L, benefit.getCouponId());
    }

    @Test
    void claimMonthlyBenefit_inactivePlanForActiveMember_stillCreatesBenefitRecord() {
        BuyerMembership membership = activeMembership(LocalDateTime.now().plusDays(20));
        MembershipPlan plan = plan();
        plan.setStatus(MembershipPlanStatus.INACTIVE);
        membership.setPlan(plan);
        BuyerCoupon buyerCoupon = BuyerCoupon.builder()
                .id(30L)
                .buyerId(2L)
                .couponId(10L)
                .status(BuyerCouponStatus.UNUSED)
                .build();
        when(buyerMembershipRepository.findByBuyerIdWithPlan(2L)).thenReturn(Optional.of(membership));
        when(membershipMonthlyBenefitRepository.existsByBuyerIdAndPlanIdAndBenefitMonth(eq(2L), eq(1L), anyString())).thenReturn(false);
        when(buyerCouponService.claimMembershipCoupon(2L, 10L)).thenReturn(buyerCoupon);
        when(membershipMonthlyBenefitRepository.save(any(MembershipMonthlyBenefit.class))).thenAnswer(i -> {
            MembershipMonthlyBenefit benefit = i.getArgument(0);
            benefit.setId(40L);
            return benefit;
        });

        MembershipMonthlyBenefit benefit = membershipService.claimMonthlyBenefit(2L);

        assertEquals(40L, benefit.getId());
        assertEquals(30L, benefit.getBuyerCouponId());
        verify(buyerCouponService).claimMembershipCoupon(2L, 10L);
    }

    @Test
    void claimMonthlyBenefit_duplicateOrExpired_throws() {
        BuyerMembership membership = activeMembership(LocalDateTime.now().plusDays(20));
        membership.setPlan(plan());
        when(buyerMembershipRepository.findByBuyerIdWithPlan(2L)).thenReturn(Optional.of(membership));
        when(membershipMonthlyBenefitRepository.existsByBuyerIdAndPlanIdAndBenefitMonth(eq(2L), eq(1L), anyString())).thenReturn(true);

        BusinessException duplicate = assertThrows(BusinessException.class, () -> membershipService.claimMonthlyBenefit(2L));
        assertEquals(400, duplicate.getStatus());

        BuyerMembership expired = activeMembership(LocalDateTime.now().minusDays(1));
        when(buyerMembershipRepository.findByBuyerIdWithPlan(3L)).thenReturn(Optional.of(expired));
        BusinessException expiredEx = assertThrows(BusinessException.class, () -> membershipService.claimMonthlyBenefit(3L));
        assertEquals(400, expiredEx.getStatus());
        verify(buyerCouponService, never()).claimMembershipCoupon(anyLong(), anyLong());
    }

    @Test
    void status_activeMembership_returnsClaimedCurrentMonthFlag() {
        BuyerMembership membership = activeMembership(LocalDateTime.now().plusDays(20));
        membership.setPlan(plan());
        when(buyerMembershipRepository.findByBuyerIdWithPlan(2L)).thenReturn(Optional.of(membership));
        when(membershipMonthlyBenefitRepository.existsByBuyerIdAndPlanIdAndBenefitMonth(eq(2L), eq(1L), anyString()))
                .thenReturn(true);

        MembershipService.MembershipStatus status = membershipService.status(2L);

        assertSame(membership, status.membership());
        assertEquals(BuyerMembershipStatus.ACTIVE, status.displayStatus());
        assertTrue(status.currentMonthBenefitClaimed());
    }

    @Test
    void findActiveMembership_expiredMembership_returnsEmpty() {
        BuyerMembership membership = activeMembership(LocalDateTime.now().minusDays(1));
        membership.setPlan(plan());
        when(buyerMembershipRepository.findByBuyerIdWithPlan(2L)).thenReturn(Optional.of(membership));

        assertTrue(membershipService.findActiveMembership(2L).isEmpty());
    }

    @Test
    void membershipMonthlyBenefit_hasBuyerPlanMonthUniqueConstraint() {
        Table table = MembershipMonthlyBenefit.class.getAnnotation(Table.class);

        assertNotNull(table);
        assertTrue(Arrays.stream(table.uniqueConstraints()).anyMatch(constraint -> {
            List<String> columns = Arrays.asList(constraint.columnNames());
            return columns.size() == 3
                    && columns.contains("buyer_id")
                    && columns.contains("plan_id")
                    && columns.contains("benefit_month");
        }));
    }

    @Test
    void prepareOrderDiscount_activeMembership_returnsDiscountSnapshot() {
        BuyerMembership membership = activeMembership(LocalDateTime.now().plusDays(20));
        membership.setPlan(plan());
        when(buyerMembershipRepository.findByBuyerIdWithPlan(2L)).thenReturn(Optional.of(membership));

        MembershipService.OrderMembershipDiscount discount =
                membershipService.prepareOrderDiscount(2L, BigDecimal.valueOf(100));

        assertNotNull(discount);
        assertEquals(1L, discount.planId());
        assertEquals(BigDecimal.valueOf(0.95), discount.discountRate());
        assertEquals(new BigDecimal("5.00"), discount.discountAmount());
    }

    @Test
    void listActivePlans_returnsOnlyActivePlans() {
        when(membershipPlanRepository.findByStatusOrderByPriceAscIdDesc(MembershipPlanStatus.ACTIVE))
                .thenReturn(List.of(plan()));

        assertEquals(1, membershipService.listActivePlans().size());
    }

    private MembershipPlan plan() {
        return MembershipPlan.builder()
                .id(1L)
                .name("月度会员")
                .price(BigDecimal.valueOf(30))
                .durationMonths(1)
                .discountRate(BigDecimal.valueOf(0.95))
                .monthlyCouponId(10L)
                .status(MembershipPlanStatus.ACTIVE)
                .build();
    }

    private MembershipPurchase pendingPurchase() {
        return MembershipPurchase.builder()
                .id(20L)
                .buyerId(2L)
                .planId(1L)
                .amount(BigDecimal.valueOf(30))
                .outTradeNo("MEM-20")
                .status(MembershipPurchaseStatus.PENDING_PAYMENT)
                .build();
    }

    private BuyerMembership activeMembership(LocalDateTime expiresAt) {
        return BuyerMembership.builder()
                .id(5L)
                .buyerId(2L)
                .planId(1L)
                .status(BuyerMembershipStatus.ACTIVE)
                .startedAt(LocalDateTime.now().minusDays(1))
                .expiresAt(expiresAt)
                .lastPaidAt(LocalDateTime.now().minusDays(1))
                .build();
    }
}
