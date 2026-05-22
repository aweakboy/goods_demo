package com.trading.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trading.dto.PriceAlertRequest;
import com.trading.dto.PriceAlertResponse;
import com.trading.entity.PriceAlert;
import com.trading.entity.User;
import com.trading.enums.PriceAlertStatus;
import com.trading.enums.Role;
import com.trading.security.JwtUtil;
import com.trading.security.UserDetailsServiceImpl;
import com.trading.service.PriceAlertService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BuyerPriceAlertController.class)
@Import(BuyerPriceAlertControllerTest.TestSecurityConfig.class)
class BuyerPriceAlertControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean PriceAlertService priceAlertService;
    @MockBean JwtUtil jwtUtil;
    @MockBean UserDetailsServiceImpl userDetailsService;

    @Test
    void mine_buyer_returnsAlerts() throws Exception {
        when(priceAlertService.listAlerts(2L, 0, 20))
                .thenReturn(new PageImpl<>(List.of(alertResponse()), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/buyer/price-alerts")
                        .with(SecurityMockMvcRequestPostProcessors.user(buyerPrincipal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].productId").value(10));
    }

    @Test
    void createOrUpdate_buyer_returnsCreatedAlert() throws Exception {
        when(priceAlertService.createOrUpdateAlert(eqLong(2L), eqLong(10L), any(BigDecimal.class))).thenReturn(alert());

        mockMvc.perform(post("/api/v1/buyer/price-alerts/products/10")
                        .with(SecurityMockMvcRequestPostProcessors.user(buyerPrincipal()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    void cancel_buyer_returnsOk() throws Exception {
        mockMvc.perform(delete("/api/v1/buyer/price-alerts/products/10")
                        .with(SecurityMockMvcRequestPostProcessors.user(buyerPrincipal())))
                .andExpect(status().isOk());
    }

    @Test
    void mine_nonBuyer_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/buyer/price-alerts")
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

    private static Long eqLong(Long value) {
        return org.mockito.ArgumentMatchers.eq(value);
    }

    private User buyerPrincipal() {
        return User.builder().id(2L).username("buyer").email("buyer@example.com").password("pwd").role(Role.BUYER).build();
    }

    private User sellerPrincipal() {
        return User.builder().id(3L).username("seller").email("seller@example.com").password("pwd").role(Role.SELLER).build();
    }

    private PriceAlertRequest request() {
        PriceAlertRequest request = new PriceAlertRequest();
        request.setTargetPrice(BigDecimal.valueOf(80));
        return request;
    }

    private PriceAlert alert() {
        return PriceAlert.builder()
                .id(1L)
                .buyerId(2L)
                .productId(10L)
                .targetPrice(BigDecimal.valueOf(80))
                .status(PriceAlertStatus.ACTIVE)
                .build();
    }

    private PriceAlertResponse alertResponse() {
        return PriceAlertResponse.builder()
                .id(1L)
                .productId(10L)
                .targetPrice(BigDecimal.valueOf(80))
                .status(PriceAlertStatus.ACTIVE)
                .build();
    }
}
