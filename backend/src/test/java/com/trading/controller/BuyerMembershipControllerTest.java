package com.trading.controller;

import com.trading.entity.*;
import com.trading.enums.BuyerMembershipStatus;
import com.trading.enums.BuyerCouponStatus;
import com.trading.enums.MembershipPlanStatus;
import com.trading.enums.MembershipPurchaseStatus;
import com.trading.enums.Role;
import com.trading.security.JwtUtil;
import com.trading.security.UserDetailsServiceImpl;
import com.trading.service.MembershipService;
import com.trading.service.PaymentService;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BuyerMembershipController.class)
@Import(BuyerMembershipControllerTest.TestSecurityConfig.class)
class BuyerMembershipControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean MembershipService membershipService;
    @MockBean PaymentService paymentService;
    @MockBean JwtUtil jwtUtil;
    @MockBean UserDetailsServiceImpl userDetailsService;

    @Test
    void plans_buyer_returnsActivePlans() throws Exception {
        when(membershipService.listActivePlans()).thenReturn(List.of(plan()));

        mockMvc.perform(get("/api/v1/buyer/membership/plans")
                        .with(SecurityMockMvcRequestPostProcessors.user(buyerPrincipal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].status").value("ACTIVE"));
    }

    @Test
    void status_buyer_returnsCurrentMembership() throws Exception {
        BuyerMembership membership = membership();
        membership.setPlan(plan());
        when(membershipService.status(2L))
                .thenReturn(new MembershipService.MembershipStatus(membership, BuyerMembershipStatus.ACTIVE, true));

        mockMvc.perform(get("/api/v1/buyer/membership/status")
                        .with(SecurityMockMvcRequestPostProcessors.user(buyerPrincipal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.member").value(true))
                .andExpect(jsonPath("$.data.planId").value(1))
                .andExpect(jsonPath("$.data.currentMonthBenefitClaimed").value(true));
    }

    @Test
    void purchase_buyer_returnsPaymentHtml() throws Exception {
        MembershipPurchase purchase = purchase();
        purchase.setPlan(plan());
        when(membershipService.createPurchase(2L, 1L)).thenReturn(purchase);
        when(paymentService.createPayForm(eq("MEM-20"), eq(BigDecimal.valueOf(30)), anyString()))
                .thenReturn("<form id=\"pay\"></form>");

        mockMvc.perform(post("/api/v1/buyer/membership/plans/1/purchase")
                        .with(SecurityMockMvcRequestPostProcessors.user(buyerPrincipal())))
                .andExpect(status().isOk())
                .andExpect(content().string("<form id=\"pay\"></form>"));
    }

    @Test
    void claimMonthlyBenefit_buyer_returnsBenefit() throws Exception {
        when(membershipService.claimMonthlyBenefit(2L)).thenReturn(benefit());

        mockMvc.perform(post("/api/v1/buyer/membership/benefits/monthly/claim")
                        .with(SecurityMockMvcRequestPostProcessors.user(buyerPrincipal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.data.buyerCouponId").value(30));
    }

    @Test
    void purchase_nonBuyer_returnsForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/buyer/membership/plans/1/purchase")
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

    private MembershipPlan plan() {
        return MembershipPlan.builder()
                .id(1L)
                .name("Monthly Member")
                .price(BigDecimal.valueOf(30))
                .durationMonths(1)
                .discountRate(BigDecimal.valueOf(0.95))
                .monthlyCouponId(10L)
                .status(MembershipPlanStatus.ACTIVE)
                .build();
    }

    private BuyerMembership membership() {
        return BuyerMembership.builder()
                .id(5L)
                .buyerId(2L)
                .planId(1L)
                .status(BuyerMembershipStatus.ACTIVE)
                .startedAt(LocalDateTime.now().minusDays(1))
                .expiresAt(LocalDateTime.now().plusDays(29))
                .lastPaidAt(LocalDateTime.now().minusDays(1))
                .build();
    }

    private MembershipPurchase purchase() {
        return MembershipPurchase.builder()
                .id(20L)
                .buyerId(2L)
                .planId(1L)
                .amount(BigDecimal.valueOf(30))
                .outTradeNo("MEM-20")
                .status(MembershipPurchaseStatus.PENDING_PAYMENT)
                .build();
    }

    private MembershipMonthlyBenefit benefit() {
        BuyerCoupon buyerCoupon = BuyerCoupon.builder()
                .id(30L)
                .buyerId(2L)
                .couponId(10L)
                .status(BuyerCouponStatus.UNUSED)
                .build();
        MembershipMonthlyBenefit benefit = MembershipMonthlyBenefit.builder()
                .id(40L)
                .buyerId(2L)
                .planId(1L)
                .benefitMonth("2026-05")
                .couponId(10L)
                .buyerCouponId(30L)
                .claimedAt(LocalDateTime.now())
                .build();
        benefit.setBuyerCoupon(buyerCoupon);
        return benefit;
    }
}
