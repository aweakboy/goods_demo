package com.trading.service;

import com.trading.common.BusinessException;
import com.trading.entity.BuyerCoupon;
import com.trading.entity.Coupon;
import com.trading.enums.BuyerCouponStatus;
import com.trading.enums.CouponAudience;
import com.trading.enums.CouponStatus;
import com.trading.repository.BuyerCouponRepository;
import com.trading.repository.CouponRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BuyerCouponServiceTest {

    @Mock CouponRepository couponRepository;
    @Mock BuyerCouponRepository buyerCouponRepository;

    @InjectMocks BuyerCouponService buyerCouponService;

    @Test
    void claim_success_createsUnusedBuyerCouponAndIncrementsClaimedQuantity() {
        Coupon coupon = validCoupon();
        when(couponRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(coupon));
        when(buyerCouponRepository.countByBuyerIdAndCouponId(2L, 1L)).thenReturn(0L);
        when(couponRepository.save(any(Coupon.class))).thenAnswer(i -> i.getArgument(0));
        when(buyerCouponRepository.save(any(BuyerCoupon.class))).thenAnswer(i -> {
            BuyerCoupon buyerCoupon = i.getArgument(0);
            buyerCoupon.setId(10L);
            return buyerCoupon;
        });

        BuyerCoupon buyerCoupon = buyerCouponService.claim(2L, 1L);

        assertEquals(10L, buyerCoupon.getId());
        assertEquals(BuyerCouponStatus.UNUSED, buyerCoupon.getStatus());
        assertEquals(1, coupon.getClaimedQuantity());
        verify(couponRepository).save(coupon);
        verify(buyerCouponRepository).save(any(BuyerCoupon.class));
    }

    @Test
    void listClaimableCoupons_usesPublicAudienceOnly() {
        Coupon coupon = validCoupon();
        when(couponRepository.findClaimable(eq(CouponStatus.ACTIVE), eq(CouponAudience.PUBLIC), any()))
                .thenReturn(List.of(coupon));

        List<Coupon> coupons = buyerCouponService.listClaimableCoupons();

        assertEquals(List.of(coupon), coupons);
        verify(couponRepository).findClaimable(eq(CouponStatus.ACTIVE), eq(CouponAudience.PUBLIC), any());
    }

    @Test
    void claim_memberAudienceCoupon_rejectsPublicClaim() {
        Coupon coupon = validCoupon();
        coupon.setAudience(CouponAudience.MEMBER);
        when(couponRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(coupon));

        BusinessException ex = assertThrows(BusinessException.class, () -> buyerCouponService.claim(2L, 1L));

        assertEquals(400, ex.getStatus());
        verify(couponRepository, never()).save(any());
        verify(buyerCouponRepository, never()).save(any());
    }

    @Test
    void claimMembershipCoupon_memberCoupon_success() {
        Coupon coupon = validCoupon();
        coupon.setAudience(CouponAudience.MEMBER);
        when(couponRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(coupon));
        when(couponRepository.save(any(Coupon.class))).thenAnswer(i -> i.getArgument(0));
        when(buyerCouponRepository.save(any(BuyerCoupon.class))).thenAnswer(i -> {
            BuyerCoupon buyerCoupon = i.getArgument(0);
            buyerCoupon.setId(20L);
            return buyerCoupon;
        });

        BuyerCoupon buyerCoupon = buyerCouponService.claimMembershipCoupon(2L, 1L);

        assertEquals(20L, buyerCoupon.getId());
        assertEquals(BuyerCouponStatus.UNUSED, buyerCoupon.getStatus());
        assertEquals(1, coupon.getClaimedQuantity());
        verify(buyerCouponRepository, never()).countByBuyerIdAndCouponId(anyLong(), anyLong());
    }

    @Test
    void claim_soldOut_doesNotCreateBuyerCoupon() {
        Coupon coupon = validCoupon();
        coupon.setClaimedQuantity(10);
        coupon.setTotalQuantity(10);
        when(couponRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(coupon));

        BusinessException ex = assertThrows(BusinessException.class, () -> buyerCouponService.claim(2L, 1L));

        assertEquals(400, ex.getStatus());
        assertTrue(ex.getMessage().contains("已领完"));
        verify(buyerCouponRepository, never()).save(any());
    }

    @Test
    void claim_overPerUserLimit_doesNotCreateBuyerCoupon() {
        Coupon coupon = validCoupon();
        when(couponRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(coupon));
        when(buyerCouponRepository.countByBuyerIdAndCouponId(2L, 1L)).thenReturn(1L);

        BusinessException ex = assertThrows(BusinessException.class, () -> buyerCouponService.claim(2L, 1L));

        assertEquals(400, ex.getStatus());
        assertTrue(ex.getMessage().contains("领取上限"));
        verify(buyerCouponRepository, never()).save(any());
    }

    @Test
    void claim_inactiveOrExpired_doesNotCreateBuyerCoupon() {
        Coupon inactive = validCoupon();
        inactive.setStatus(CouponStatus.INACTIVE);
        when(couponRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(inactive));
        BusinessException inactiveEx = assertThrows(BusinessException.class, () -> buyerCouponService.claim(2L, 1L));
        assertEquals(400, inactiveEx.getStatus());

        Coupon expired = validCoupon();
        expired.setValidTo(LocalDateTime.now().minusMinutes(1));
        when(couponRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(expired));
        BusinessException expiredEx = assertThrows(BusinessException.class, () -> buyerCouponService.claim(2L, 1L));
        assertEquals(400, expiredEx.getStatus());
        verify(buyerCouponRepository, never()).save(any());
    }

    @Test
    void displayStatus_unusedExpiredCoupon_returnsExpired() {
        BuyerCoupon buyerCoupon = BuyerCoupon.builder()
                .id(10L)
                .buyerId(2L)
                .couponId(1L)
                .status(BuyerCouponStatus.UNUSED)
                .build();
        Coupon coupon = validCoupon();
        coupon.setValidTo(LocalDateTime.now().minusDays(1));
        buyerCoupon.setCoupon(coupon);

        assertEquals(BuyerCouponStatus.EXPIRED, buyerCouponService.displayStatus(buyerCoupon));
    }

    @Test
    void listMine_usesCurrentBuyerOnly() {
        BuyerCoupon buyerCoupon = buyerCoupon();
        when(buyerCouponRepository.findByBuyerIdWithCoupon(2L)).thenReturn(List.of(buyerCoupon));

        List<BuyerCoupon> result = buyerCouponService.listMine(2L, null);

        assertEquals(1, result.size());
        assertEquals(2L, result.get(0).getBuyerId());
    }

    @Test
    void prepareForOrder_success_returnsCouponUsage() {
        BuyerCoupon buyerCoupon = buyerCoupon();
        when(buyerCouponRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(buyerCoupon));

        BuyerCouponService.CouponUsage usage =
                buyerCouponService.prepareForOrder(2L, 10L, BigDecimal.valueOf(120));

        assertEquals(1L, usage.couponId());
        assertEquals(10L, usage.buyerCouponId());
        assertEquals(BigDecimal.valueOf(10), usage.appliedDiscount());
    }

    @Test
    void prepareForOrder_invalidOwnershipOrThreshold_throws() {
        BuyerCoupon foreign = buyerCoupon();
        foreign.setBuyerId(3L);
        when(buyerCouponRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(foreign));
        BusinessException forbidden = assertThrows(BusinessException.class,
                () -> buyerCouponService.prepareForOrder(2L, 10L, BigDecimal.valueOf(120)));
        assertEquals(403, forbidden.getStatus());

        BuyerCoupon own = buyerCoupon();
        when(buyerCouponRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(own));
        BusinessException badRequest = assertThrows(BusinessException.class,
                () -> buyerCouponService.prepareForOrder(2L, 10L, BigDecimal.valueOf(80)));
        assertEquals(400, badRequest.getStatus());
    }

    @Test
    void markUsed_setsStatusAndOrderId() {
        BuyerCoupon buyerCoupon = buyerCoupon();
        BuyerCouponService.CouponUsage usage = new BuyerCouponService.CouponUsage(
                buyerCoupon,
                1L,
                10L,
                "满100减10",
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(10),
                BigDecimal.valueOf(10)
        );

        buyerCouponService.markUsed(usage, 99L);

        assertEquals(BuyerCouponStatus.USED, buyerCoupon.getStatus());
        assertEquals(99L, buyerCoupon.getUsedOrderId());
        assertNotNull(buyerCoupon.getUsedAt());
        verify(buyerCouponRepository).save(buyerCoupon);
    }

    @Test
    void releaseForOrder_usedByOrder_restoresUnusedAndClearsUsage() {
        BuyerCoupon buyerCoupon = usedBuyerCoupon(99L);
        buyerCoupon.getCoupon().setClaimedQuantity(3);
        when(buyerCouponRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(buyerCoupon));

        buyerCouponService.releaseForOrder(10L, 99L);

        assertEquals(BuyerCouponStatus.UNUSED, buyerCoupon.getStatus());
        assertNull(buyerCoupon.getUsedAt());
        assertNull(buyerCoupon.getUsedOrderId());
        assertEquals(3, buyerCoupon.getCoupon().getClaimedQuantity());
        verify(buyerCouponRepository).save(buyerCoupon);
    }

    @Test
    void releaseForOrder_alreadyReleased_isIdempotent() {
        BuyerCoupon buyerCoupon = buyerCoupon();
        when(buyerCouponRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(buyerCoupon));

        buyerCouponService.releaseForOrder(10L, 99L);

        assertEquals(BuyerCouponStatus.UNUSED, buyerCoupon.getStatus());
        assertNull(buyerCoupon.getUsedOrderId());
        verify(buyerCouponRepository, never()).save(any());
    }

    @Test
    void releaseForOrder_usedByDifferentOrder_doesNotModifyCoupon() {
        BuyerCoupon buyerCoupon = usedBuyerCoupon(88L);
        LocalDateTime usedAt = buyerCoupon.getUsedAt();
        when(buyerCouponRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(buyerCoupon));

        buyerCouponService.releaseForOrder(10L, 99L);

        assertEquals(BuyerCouponStatus.USED, buyerCoupon.getStatus());
        assertEquals(88L, buyerCoupon.getUsedOrderId());
        assertEquals(usedAt, buyerCoupon.getUsedAt());
        verify(buyerCouponRepository, never()).save(any());
    }

    @Test
    void releaseForOrder_expiredCouponDisplaysExpiredAndIsNotUsable() {
        BuyerCoupon buyerCoupon = usedBuyerCoupon(99L);
        buyerCoupon.getCoupon().setValidTo(LocalDateTime.now().minusDays(1));
        when(buyerCouponRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(buyerCoupon));

        buyerCouponService.releaseForOrder(10L, 99L);
        when(buyerCouponRepository.findByBuyerIdAndStatusWithCoupon(2L, BuyerCouponStatus.UNUSED))
                .thenReturn(List.of(buyerCoupon));

        assertEquals(BuyerCouponStatus.EXPIRED, buyerCouponService.displayStatus(buyerCoupon));
        assertTrue(buyerCouponService.listUsable(2L).isEmpty());
    }

    private Coupon validCoupon() {
        return Coupon.builder()
                .id(1L)
                .name("满100减10")
                .thresholdAmount(BigDecimal.valueOf(100))
                .discountAmount(BigDecimal.valueOf(10))
                .totalQuantity(10)
                .claimedQuantity(0)
                .perUserLimit(1)
                .validFrom(LocalDateTime.now().minusDays(1))
                .validTo(LocalDateTime.now().plusDays(7))
                .status(CouponStatus.ACTIVE)
                .audience(CouponAudience.PUBLIC)
                .createdBy(9L)
                .build();
    }

    private BuyerCoupon buyerCoupon() {
        BuyerCoupon buyerCoupon = BuyerCoupon.builder()
                .id(10L)
                .buyerId(2L)
                .couponId(1L)
                .status(BuyerCouponStatus.UNUSED)
                .build();
        buyerCoupon.setCoupon(validCoupon());
        return buyerCoupon;
    }

    private BuyerCoupon usedBuyerCoupon(Long usedOrderId) {
        BuyerCoupon buyerCoupon = buyerCoupon();
        buyerCoupon.setStatus(BuyerCouponStatus.USED);
        buyerCoupon.setUsedAt(LocalDateTime.now().minusMinutes(5));
        buyerCoupon.setUsedOrderId(usedOrderId);
        return buyerCoupon;
    }
}
