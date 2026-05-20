package com.trading.service;

import com.trading.common.BusinessException;
import com.trading.dto.RegisterRequest;
import com.trading.dto.LoginRequest;
import com.trading.entity.User;
import com.trading.enums.Role;
import com.trading.repository.UserRepository;
import com.trading.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtUtil jwtUtil;

    @InjectMocks AuthService authService;

    @BeforeEach
    void setUp() { MockitoAnnotations.openMocks(this); }

    @Test
    void register_duplicateEmail_throwsBadRequest() {
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);
        RegisterRequest req = new RegisterRequest();
        req.setEmail("test@example.com");
        req.setUsername("user");
        req.setPassword("pass1234");
        req.setRole(Role.BUYER);
        BusinessException ex = assertThrows(BusinessException.class, () -> authService.register(req));
        assertEquals(400, ex.getStatus());
    }

    @Test
    void login_wrongPassword_throwsUnauthorized() {
        User user = User.builder().email("u@e.com").password("encoded").role(Role.BUYER).build();
        when(userRepository.findByEmail("u@e.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "encoded")).thenReturn(false);
        LoginRequest req = new LoginRequest();
        req.setEmail("u@e.com");
        req.setPassword("wrong");
        BusinessException ex = assertThrows(BusinessException.class, () -> authService.login(req));
        assertEquals(401, ex.getStatus());
    }

    @Test
    void login_success_returnsTokens() {
        User user = User.builder().id(1L).email("u@e.com").password("encoded")
                .role(Role.BUYER).username("user1").build();
        when(userRepository.findByEmail("u@e.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("pass1234", "encoded")).thenReturn(true);
        when(jwtUtil.generateAccessToken(any(), any())).thenReturn("access_token");
        when(jwtUtil.generateRefreshToken(any())).thenReturn("refresh_token");
        LoginRequest req = new LoginRequest();
        req.setEmail("u@e.com");
        req.setPassword("pass1234");
        var result = authService.login(req);
        assertEquals("access_token", result.getAccessToken());
        assertEquals("BUYER", result.getRole());
    }
}
