package com.trading.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trading.dto.BuyerAddressRequest;
import com.trading.entity.BuyerAddress;
import com.trading.entity.User;
import com.trading.enums.AddressValidationStatus;
import com.trading.enums.Role;
import com.trading.security.JwtUtil;
import com.trading.security.UserDetailsServiceImpl;
import com.trading.service.BuyerAddressService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BuyerAddressController.class)
@Import(BuyerAddressControllerTest.TestSecurityConfig.class)
class BuyerAddressControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean BuyerAddressService buyerAddressService;
    @MockBean JwtUtil jwtUtil;
    @MockBean UserDetailsServiceImpl userDetailsService;

    @Test
    void list_buyer_returnsAddresses() throws Exception {
        when(buyerAddressService.list(1L)).thenReturn(List.of(validAddress()));

        mockMvc.perform(get("/api/v1/buyer/addresses")
                        .with(SecurityMockMvcRequestPostProcessors.user(buyerPrincipal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(10))
                .andExpect(jsonPath("$.data[0].defaultAddress").value(true));
    }

    @Test
    void create_buyer_returnsCreatedAddress() throws Exception {
        BuyerAddressRequest request = validRequest();
        when(buyerAddressService.create(eq(1L), any(BuyerAddressRequest.class))).thenReturn(validAddress());

        mockMvc.perform(post("/api/v1/buyer/addresses")
                        .with(SecurityMockMvcRequestPostProcessors.user(buyerPrincipal()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.data.receiverName").value("张三"));
    }

    @Test
    void list_nonBuyer_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/buyer/addresses")
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
                .id(1L)
                .username("buyer")
                .email("buyer@example.com")
                .password("pwd")
                .role(Role.BUYER)
                .build();
    }

    private User sellerPrincipal() {
        return User.builder()
                .id(2L)
                .username("seller")
                .email("seller@example.com")
                .password("pwd")
                .role(Role.SELLER)
                .build();
    }

    private BuyerAddressRequest validRequest() {
        BuyerAddressRequest request = new BuyerAddressRequest();
        request.setReceiverName("张三");
        request.setReceiverPhone("13800138000");
        request.setProvince("浙江省");
        request.setCity("杭州市");
        request.setDistrict("西湖区");
        request.setDetailAddress("文三路100号");
        return request;
    }

    private BuyerAddress validAddress() {
        return BuyerAddress.builder()
                .id(10L)
                .buyerId(1L)
                .receiverName("张三")
                .receiverPhone("13800138000")
                .province("浙江省")
                .city("杭州市")
                .district("西湖区")
                .detailAddress("文三路100号")
                .fullAddress("浙江省杭州市西湖区文三路100号")
                .validationStatus(AddressValidationStatus.VALID)
                .defaultAddress(true)
                .build();
    }
}
