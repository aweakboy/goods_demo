package com.trading.service;

import com.trading.common.BusinessException;
import com.trading.dto.MembershipPlanRequest;
import com.trading.entity.Coupon;
import com.trading.entity.MembershipPlan;
import com.trading.enums.CouponAudience;
import com.trading.enums.CouponStatus;
import com.trading.enums.MembershipPlanStatus;
import com.trading.repository.CouponRepository;
import com.trading.repository.MembershipPlanRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MembershipAdminServiceTest {

    @Mock MembershipPlanRepository membershipPlanRepository;
    @Mock CouponRepository couponRepository;

    @InjectMocks MembershipAdminService membershipAdminService;

    @Test
    void create_validPlan_succeeds() {
        MembershipPlanRequest request = validRequest();
        when(couponRepository.findById(10L)).thenReturn(Optional.of(memberCoupon()));
        when(membershipPlanRepository.save(any(MembershipPlan.class))).thenAnswer(i -> {
            MembershipPlan plan = i.getArgument(0);
            plan.setId(1L);
            return plan;
        });

        MembershipPlan plan = membershipAdminService.create(request);

        assertEquals(1L, plan.getId());
        assertEquals("月度会员", plan.getName());
        assertEquals(BigDecimal.valueOf(0.95), plan.getDiscountRate());
        assertEquals(MembershipPlanStatus.ACTIVE, plan.getStatus());
    }

    @Test
    void create_invalidFields_throwsBadRequest() {
        MembershipPlanRequest invalidPrice = validRequest();
        invalidPrice.setPrice(BigDecimal.ZERO);
        BusinessException priceEx = assertThrows(BusinessException.class,
                () -> membershipAdminService.create(invalidPrice));
        assertEquals(400, priceEx.getStatus());

        MembershipPlanRequest invalidDiscount = validRequest();
        invalidDiscount.setDiscountRate(BigDecimal.valueOf(1.1));
        BusinessException discountEx = assertThrows(BusinessException.class,
                () -> membershipAdminService.create(invalidDiscount));
        assertEquals(400, discountEx.getStatus());

        MembershipPlanRequest invalidDuration = validRequest();
        invalidDuration.setDurationMonths(0);
        BusinessException durationEx = assertThrows(BusinessException.class,
                () -> membershipAdminService.create(invalidDuration));
        assertEquals(400, durationEx.getStatus());

        verify(membershipPlanRepository, never()).save(any());
    }

    @Test
    void create_missingMonthlyCoupon_throwsBadRequest() {
        MembershipPlanRequest request = validRequest();
        when(couponRepository.findById(10L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () -> membershipAdminService.create(request));

        assertEquals(400, ex.getStatus());
        verify(membershipPlanRepository, never()).save(any());
    }

    @Test
    void create_nonMemberCoupon_throwsBadRequest() {
        MembershipPlanRequest request = validRequest();
        Coupon coupon = memberCoupon();
        coupon.setAudience(CouponAudience.PUBLIC);
        when(couponRepository.findById(10L)).thenReturn(Optional.of(coupon));

        BusinessException ex = assertThrows(BusinessException.class, () -> membershipAdminService.create(request));

        assertEquals(400, ex.getStatus());
        verify(membershipPlanRepository, never()).save(any());
    }

    @Test
    void activateAndDeactivate_updateStatus() {
        MembershipPlan plan = validPlan();
        when(membershipPlanRepository.findById(1L)).thenReturn(Optional.of(plan));
        when(membershipPlanRepository.save(any(MembershipPlan.class))).thenAnswer(i -> i.getArgument(0));

        MembershipPlan active = membershipAdminService.activate(1L);
        assertEquals(MembershipPlanStatus.ACTIVE, active.getStatus());

        MembershipPlan inactive = membershipAdminService.deactivate(1L);
        assertEquals(MembershipPlanStatus.INACTIVE, inactive.getStatus());
    }

    private MembershipPlanRequest validRequest() {
        MembershipPlanRequest request = new MembershipPlanRequest();
        request.setName("月度会员");
        request.setDescription("会员权益");
        request.setPrice(BigDecimal.valueOf(30));
        request.setDurationMonths(1);
        request.setDiscountRate(BigDecimal.valueOf(0.95));
        request.setMonthlyCouponId(10L);
        request.setStatus(MembershipPlanStatus.ACTIVE);
        return request;
    }

    private MembershipPlan validPlan() {
        return MembershipPlan.builder()
                .id(1L)
                .name("月度会员")
                .price(BigDecimal.valueOf(30))
                .durationMonths(1)
                .discountRate(BigDecimal.valueOf(0.95))
                .status(MembershipPlanStatus.INACTIVE)
                .build();
    }

    private Coupon memberCoupon() {
        return Coupon.builder()
                .id(10L)
                .name("会员券")
                .thresholdAmount(BigDecimal.valueOf(100))
                .discountAmount(BigDecimal.valueOf(20))
                .totalQuantity(100)
                .claimedQuantity(0)
                .perUserLimit(1)
                .validFrom(LocalDateTime.now().minusDays(1))
                .validTo(LocalDateTime.now().plusDays(30))
                .status(CouponStatus.ACTIVE)
                .audience(CouponAudience.MEMBER)
                .createdBy(9L)
                .build();
    }
}
