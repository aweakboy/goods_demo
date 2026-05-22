package com.trading.controller;

import com.trading.dto.ShopFavoriteResponse;
import com.trading.dto.ShopFavoriteStateResponse;
import com.trading.entity.User;
import com.trading.enums.Role;
import com.trading.security.JwtUtil;
import com.trading.security.UserDetailsServiceImpl;
import com.trading.service.ShopFavoriteService;
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

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BuyerShopFavoriteController.class)
@Import(BuyerShopFavoriteControllerTest.TestSecurityConfig.class)
class BuyerShopFavoriteControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean ShopFavoriteService shopFavoriteService;
    @MockBean JwtUtil jwtUtil;
    @MockBean UserDetailsServiceImpl userDetailsService;

    @Test
    void mine_buyer_returnsFavorites() throws Exception {
        when(shopFavoriteService.listFavorites(2L, 0, 20))
                .thenReturn(new PageImpl<>(List.of(favoriteResponse()), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/buyer/shop-favorites")
                        .with(SecurityMockMvcRequestPostProcessors.user(buyerPrincipal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].shopId").value(10));
    }

    @Test
    void favorite_buyer_returnsState() throws Exception {
        when(shopFavoriteService.favoriteShop(2L, 10L)).thenReturn(state(true));

        mockMvc.perform(post("/api/v1/buyer/shop-favorites/10")
                        .with(SecurityMockMvcRequestPostProcessors.user(buyerPrincipal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.data.favorited").value(true));
    }

    @Test
    void unfavorite_buyer_returnsState() throws Exception {
        when(shopFavoriteService.unfavoriteShop(2L, 10L)).thenReturn(state(false));

        mockMvc.perform(delete("/api/v1/buyer/shop-favorites/10")
                        .with(SecurityMockMvcRequestPostProcessors.user(buyerPrincipal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.favorited").value(false));
    }

    @Test
    void mine_nonBuyer_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/buyer/shop-favorites")
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

    private ShopFavoriteStateResponse state(boolean favorited) {
        return ShopFavoriteStateResponse.builder().shopId(10L).favorited(favorited).favoriteCount(favorited ? 1L : 0L).build();
    }

    private ShopFavoriteResponse favoriteResponse() {
        return ShopFavoriteResponse.builder()
                .id(1L)
                .shopId(10L)
                .shopName("测试店铺")
                .accessible(true)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
