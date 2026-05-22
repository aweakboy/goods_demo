package com.trading.service;

import com.trading.common.BusinessException;
import com.trading.dto.CouponRequest;
import com.trading.entity.Coupon;
import com.trading.enums.CouponStatus;
import com.trading.repository.CouponRepository;
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
class CouponAdminServiceTest {

    @Mock CouponRepository couponRepository;

    @InjectMocks CouponAdminService couponAdminService;

    @Test
    void create_validCoupon_succeedsWithZeroClaimedQuantity() {
        CouponRequest request = validRequest();
        when(couponRepository.save(any(Coupon.class))).thenAnswer(i -> {
            Coupon coupon = i.getArgument(0);
            coupon.setId(1L);
            return coupon;
        });

        Coupon coupon = couponAdminService.create(9L, request);

        assertEquals(1L, coupon.getId());
        assertEquals("满100减10", coupon.getName());
        assertEquals(0, coupon.getClaimedQuantity());
        assertEquals(CouponStatus.ACTIVE, coupon.getStatus());
        assertFalse(coupon.getStackable());
        assertEquals(9L, coupon.getCreatedBy());
    }

    @Test
    void createAndUpdate_stackableTrue_savesConfiguration() {
        CouponRequest request = validRequest();
        request.setStackable(true);
        when(couponRepository.save(any(Coupon.class))).thenAnswer(i -> {
            Coupon coupon = i.getArgument(0);
            coupon.setId(1L);
            return coupon;
        });

        Coupon created = couponAdminService.create(9L, request);

        assertTrue(created.getStackable());

        Coupon existing = validCoupon();
        request.setName("满100减20");
        when(couponRepository.findById(1L)).thenReturn(Optional.of(existing));

        Coupon updated = couponAdminService.update(1L, request);

        assertEquals("满100减20", updated.getName());
        assertTrue(updated.getStackable());
    }

    @Test
    void create_discountGreaterThanThreshold_throwsBadRequest() {
        CouponRequest request = validRequest();
        request.setDiscountAmount(BigDecimal.valueOf(101));

        BusinessException ex = assertThrows(BusinessException.class, () -> couponAdminService.create(9L, request));

        assertEquals(400, ex.getStatus());
        verify(couponRepository, never()).save(any());
    }

    @Test
    void create_invalidDateRange_throwsBadRequest() {
        CouponRequest request = validRequest();
        request.setValidTo(request.getValidFrom());

        BusinessException ex = assertThrows(BusinessException.class, () -> couponAdminService.create(9L, request));

        assertEquals(400, ex.getStatus());
        verify(couponRepository, never()).save(any());
    }

    @Test
    void update_totalQuantityLessThanClaimed_throwsBadRequest() {
        Coupon existing = validCoupon();
        existing.setClaimedQuantity(5);
        CouponRequest request = validRequest();
        request.setTotalQuantity(4);
        when(couponRepository.findById(1L)).thenReturn(Optional.of(existing));

        BusinessException ex = assertThrows(BusinessException.class, () -> couponAdminService.update(1L, request));

        assertEquals(400, ex.getStatus());
        verify(couponRepository, never()).save(any());
    }

    @Test
    void activateAndDeactivate_updateStatus() {
        Coupon coupon = validCoupon();
        coupon.setStatus(CouponStatus.DRAFT);
        when(couponRepository.findById(1L)).thenReturn(Optional.of(coupon));
        when(couponRepository.save(any(Coupon.class))).thenAnswer(i -> i.getArgument(0));

        Coupon active = couponAdminService.activate(1L);
        assertEquals(CouponStatus.ACTIVE, active.getStatus());

        Coupon inactive = couponAdminService.deactivate(1L);
        assertEquals(CouponStatus.INACTIVE, inactive.getStatus());
    }

    private CouponRequest validRequest() {
        CouponRequest request = new CouponRequest();
        request.setName("满100减10");
        request.setDescription("测试优惠券");
        request.setThresholdAmount(BigDecimal.valueOf(100));
        request.setDiscountAmount(BigDecimal.valueOf(10));
        request.setTotalQuantity(100);
        request.setPerUserLimit(1);
        request.setValidFrom(LocalDateTime.now().minusDays(1));
        request.setValidTo(LocalDateTime.now().plusDays(7));
        request.setStatus(CouponStatus.ACTIVE);
        return request;
    }

    private Coupon validCoupon() {
        return Coupon.builder()
                .id(1L)
                .name("满100减10")
                .thresholdAmount(BigDecimal.valueOf(100))
                .discountAmount(BigDecimal.valueOf(10))
                .totalQuantity(100)
                .claimedQuantity(0)
                .perUserLimit(1)
                .validFrom(LocalDateTime.now().minusDays(1))
                .validTo(LocalDateTime.now().plusDays(7))
                .status(CouponStatus.ACTIVE)
                .createdBy(9L)
                .build();
    }
}
