package com.trading.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trading.dto.CouponRequest;
import com.trading.entity.Coupon;
import com.trading.entity.User;
import com.trading.enums.CouponStatus;
import com.trading.enums.Role;
import com.trading.security.JwtUtil;
import com.trading.security.UserDetailsServiceImpl;
import com.trading.service.CouponAdminService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminCouponController.class)
@Import(AdminCouponControllerTest.TestSecurityConfig.class)
class AdminCouponControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean CouponAdminService couponAdminService;
    @MockBean JwtUtil jwtUtil;
    @MockBean UserDetailsServiceImpl userDetailsService;

    @Test
    void list_admin_returnsCoupons() throws Exception {
        when(couponAdminService.list(isNull(), anyInt(), anyInt()))
                .thenReturn(new PageImpl<>(List.of(validCoupon()), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/admin/coupons")
                        .with(SecurityMockMvcRequestPostProcessors.user(adminPrincipal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(1));
    }

    @Test
    void create_admin_returnsCreatedCoupon() throws Exception {
        when(couponAdminService.create(eq(9L), any(CouponRequest.class))).thenReturn(validCoupon());

        mockMvc.perform(post("/api/v1/admin/coupons")
                        .with(SecurityMockMvcRequestPostProcessors.user(adminPrincipal()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.data.name").value("满100减10"));
    }

    @Test
    void list_nonAdmin_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/coupons")
                        .with(SecurityMockMvcRequestPostProcessors.user(buyerPrincipal())))
                .andExpect(status().isForbidden());
    }

    @TestConfiguration
    static class TestSecurityConfig {
        @Bean
        SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
            return http
                    .csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(auth -> auth
                            .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                            .anyRequest().authenticated()
                    )
                    .build();
        }
    }

    private User adminPrincipal() {
        return User.builder()
                .id(9L)
                .username("admin")
                .email("admin@example.com")
                .password("pwd")
                .role(Role.ADMIN)
                .build();
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
