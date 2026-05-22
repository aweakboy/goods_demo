package com.trading.controller;

import com.trading.dto.ShipmentResponse;
import com.trading.security.JwtUtil;
import com.trading.security.UserDetailsServiceImpl;
import com.trading.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminController.class)
@Import(AdminControllerRoutePlanningTest.TestSecurityConfig.class)
class AdminControllerRoutePlanningTest {

    @Autowired MockMvc mockMvc;

    @MockBean AdminService adminService;
    @MockBean ShopService shopService;
    @MockBean OrderService orderService;
    @MockBean OperationLogService operationLogService;
    @MockBean ShipmentSimulationService shipmentSimulationService;
    @MockBean ShipmentService shipmentService;
    @MockBean JwtUtil jwtUtil;
    @MockBean UserDetailsServiceImpl userDetailsService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void refreshShipmentRoute_admin_returnsShipmentResponse() throws Exception {
        when(shipmentService.refreshRoute(1L)).thenReturn(ShipmentResponse.builder().id(1L).build());

        mockMvc.perform(post("/api/v1/admin/shipments/1/route/refresh"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @WithMockUser(roles = "SELLER")
    void refreshShipmentRoute_nonAdmin_returnsForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/admin/shipments/1/route/refresh"))
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
}
