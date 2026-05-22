package com.trading.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trading.dto.MembershipPlanRequest;
import com.trading.entity.MembershipPlan;
import com.trading.entity.User;
import com.trading.enums.MembershipPlanStatus;
import com.trading.enums.Role;
import com.trading.security.JwtUtil;
import com.trading.security.UserDetailsServiceImpl;
import com.trading.service.MembershipAdminService;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminMembershipController.class)
@Import(AdminMembershipControllerTest.TestSecurityConfig.class)
class AdminMembershipControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean MembershipAdminService membershipAdminService;
    @MockBean JwtUtil jwtUtil;
    @MockBean UserDetailsServiceImpl userDetailsService;

    @Test
    void list_admin_returnsPlans() throws Exception {
        when(membershipAdminService.list(isNull(), anyInt(), anyInt()))
                .thenReturn(new PageImpl<>(List.of(plan()), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/admin/membership/plans")
                        .with(SecurityMockMvcRequestPostProcessors.user(adminPrincipal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(1))
                .andExpect(jsonPath("$.data.content[0].name").value("Monthly Member"));
    }

    @Test
    void create_admin_returnsCreatedPlan() throws Exception {
        when(membershipAdminService.create(any(MembershipPlanRequest.class))).thenReturn(plan());

        mockMvc.perform(post("/api/v1/admin/membership/plans")
                        .with(SecurityMockMvcRequestPostProcessors.user(adminPrincipal()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.data.discountRate").value(0.95));
    }

    @Test
    void activate_admin_returnsUpdatedPlan() throws Exception {
        MembershipPlan active = plan();
        active.setStatus(MembershipPlanStatus.ACTIVE);
        when(membershipAdminService.activate(1L)).thenReturn(active);

        mockMvc.perform(post("/api/v1/admin/membership/plans/1/activate")
                        .with(SecurityMockMvcRequestPostProcessors.user(adminPrincipal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    void list_nonAdmin_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/membership/plans")
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

    private MembershipPlanRequest request() {
        MembershipPlanRequest request = new MembershipPlanRequest();
        request.setName("Monthly Member");
        request.setDescription("member benefits");
        request.setPrice(BigDecimal.valueOf(30));
        request.setDurationMonths(1);
        request.setDiscountRate(BigDecimal.valueOf(0.95));
        request.setMonthlyCouponId(10L);
        request.setStatus(MembershipPlanStatus.ACTIVE);
        return request;
    }

    private MembershipPlan plan() {
        return MembershipPlan.builder()
                .id(1L)
                .name("Monthly Member")
                .description("member benefits")
                .price(BigDecimal.valueOf(30))
                .durationMonths(1)
                .discountRate(BigDecimal.valueOf(0.95))
                .monthlyCouponId(10L)
                .status(MembershipPlanStatus.ACTIVE)
                .build();
    }
}
