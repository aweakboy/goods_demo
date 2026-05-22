package com.trading.controller;

import com.trading.dto.BuyerNotificationResponse;
import com.trading.entity.User;
import com.trading.enums.BuyerNotificationType;
import com.trading.enums.Role;
import com.trading.security.JwtUtil;
import com.trading.security.UserDetailsServiceImpl;
import com.trading.service.BuyerNotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BuyerNotificationController.class)
@Import(BuyerNotificationControllerTest.TestSecurityConfig.class)
class BuyerNotificationControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean BuyerNotificationService buyerNotificationService;
    @MockBean JwtUtil jwtUtil;
    @MockBean UserDetailsServiceImpl userDetailsService;

    @Test
    void mine_buyer_returnsNotifications() throws Exception {
        when(buyerNotificationService.listNotifications(2L, 0, 20))
                .thenReturn(new PageImpl<>(List.of(notification()), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/buyer/notifications")
                        .with(SecurityMockMvcRequestPostProcessors.user(buyerPrincipal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].type").value("PRICE_DROP"));
    }

    @Test
    void unreadCount_buyer_returnsCount() throws Exception {
        when(buyerNotificationService.countUnread(2L)).thenReturn(3L);

        mockMvc.perform(get("/api/v1/buyer/notifications/unread-count")
                        .with(SecurityMockMvcRequestPostProcessors.user(buyerPrincipal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount").value(3));
    }

    @Test
    void markRead_buyer_returnsReadNotification() throws Exception {
        BuyerNotificationResponse read = notification();
        read.setRead(true);
        read.setReadAt(LocalDateTime.now());
        when(buyerNotificationService.markRead(2L, 1L)).thenReturn(read);

        mockMvc.perform(post("/api/v1/buyer/notifications/1/read")
                        .with(SecurityMockMvcRequestPostProcessors.user(buyerPrincipal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.read").value(true));
    }

    @Test
    void mine_nonBuyer_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/buyer/notifications")
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
        return User.builder().id(2L).username("buyer").email("buyer@example.com").password("pwd").role(Role.BUYER).build();
    }

    private User sellerPrincipal() {
        return User.builder().id(3L).username("seller").email("seller@example.com").password("pwd").role(Role.SELLER).build();
    }

    private BuyerNotificationResponse notification() {
        return BuyerNotificationResponse.builder()
                .id(1L)
                .type(BuyerNotificationType.PRICE_DROP)
                .title("商品降价提醒")
                .content("商品A 已降价")
                .productId(10L)
                .read(false)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
