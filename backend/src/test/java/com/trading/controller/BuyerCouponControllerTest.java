package com.trading.controller;

import com.trading.entity.BuyerCoupon;
import com.trading.entity.Coupon;
import com.trading.entity.User;
import com.trading.enums.BuyerCouponStatus;
import com.trading.enums.CouponStatus;
import com.trading.enums.Role;
import com.trading.security.JwtUtil;
import com.trading.security.UserDetailsServiceImpl;
import com.trading.service.BuyerCouponService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BuyerCouponController.class)
@Import(BuyerCouponControllerTest.TestSecurityConfig.class)
class BuyerCouponControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean BuyerCouponService buyerCouponService;
    @MockBean JwtUtil jwtUtil;
    @MockBean UserDetailsServiceImpl userDetailsService;

    @Test
    void claimable_buyer_returnsCoupons() throws Exception {
        when(buyerCouponService.listClaimableCoupons()).thenReturn(List.of(validCoupon()));
        when(buyerCouponService.countClaimedByBuyer(anyLong(), anyLong())).thenReturn(0L);

        mockMvc.perform(get("/api/v1/buyer/coupons/claimable")
                        .with(SecurityMockMvcRequestPostProcessors.user(buyerPrincipal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].claimLimitReached").value(false));
    }

    @Test
    void claim_buyer_returnsBuyerCoupon() throws Exception {
        BuyerCoupon buyerCoupon = validBuyerCoupon();
        when(buyerCouponService.claim(2L, 1L)).thenReturn(buyerCoupon);
        when(buyerCouponService.displayStatus(buyerCoupon)).thenReturn(BuyerCouponStatus.UNUSED);

        mockMvc.perform(post("/api/v1/buyer/coupons/1/claim")
                        .with(SecurityMockMvcRequestPostProcessors.user(buyerPrincipal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.status").value("UNUSED"));
    }

    @Test
    void mine_nonBuyer_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/buyer/coupons/mine")
                        .with(SecurityMockMvcRequestPostProcessors.user(sellerPrincipal())))
                .andExpect(status().isForbidden());
    }

    @TestConfiguration
    static class TestSecurityConfig {
        @Bean
        SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
            return http
                    .csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(auth -> auth
                            .requestMatchers("/api/v1/buyer/**").hasRole("BUYER")
                            .anyRequest().authenticated()
                    )
                    .build();
        }
    }

    private User buyerPrincipal() {
        return User.builder()
                .id(2L)
                .username("buyer")
                .email("buyer@example.com")
                .password("pwd")
                .role(Role.BUYER)
                .build();
    }

    private User sellerPrincipal() {
        return User.builder()
                .id(3L)
                .username("seller")
                .email("seller@example.com")
                .password("pwd")
                .role(Role.SELLER)
                .build();
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

    private BuyerCoupon validBuyerCoupon() {
        BuyerCoupon buyerCoupon = BuyerCoupon.builder()
                .id(10L)
                .buyerId(2L)
                .couponId(1L)
                .status(BuyerCouponStatus.UNUSED)
                .build();
        buyerCoupon.setCoupon(validCoupon());
        return buyerCoupon;
    }
}
