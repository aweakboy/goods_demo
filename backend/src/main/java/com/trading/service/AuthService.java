package com.trading.service;

import com.trading.annotation.OperationLog;
import com.trading.common.BusinessException;
import com.trading.dto.*;
import com.trading.entity.User;
import com.trading.repository.UserRepository;
import com.trading.security.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @OperationLog(module = "用户", action = "注册")
    public TokenResponse register(RegisterRequest req) {
        if (req.getRole() == com.trading.enums.Role.ADMIN) {
            throw BusinessException.badRequest("不支持该角色注册");
        }
        if (userRepository.existsByEmail(req.getEmail())) {
            throw BusinessException.badRequest("邮箱已存在");
        }
        if (userRepository.existsByUsername(req.getUsername())) {
            throw BusinessException.badRequest("用户名已存在");
        }
        User user = User.builder()
                .username(req.getUsername())
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .role(req.getRole())
                .build();
        user = userRepository.save(user);
        return buildTokenResponse(user);
    }

    @OperationLog(module = "用户", action = "登录")
    public TokenResponse login(LoginRequest req) {
        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> BusinessException.unauthorized("邮箱或密码错误"));
        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw BusinessException.unauthorized("邮箱或密码错误");
        }
        if (user.getStatus() == com.trading.enums.UserStatus.DISABLED) {
            throw BusinessException.unauthorized("账户已被禁用");
        }
        return buildTokenResponse(user);
    }

    public TokenResponse refresh(String refreshToken) {
        Claims claims;
        try {
            claims = jwtUtil.parseToken(refreshToken);
        } catch (Exception e) {
            throw BusinessException.unauthorized("Token 无效或已过期");
        }
        if (!jwtUtil.isRefreshToken(claims)) {
            throw BusinessException.unauthorized("不是有效的 Refresh Token");
        }
        User user = userRepository.findByEmail(claims.getSubject())
                .orElseThrow(() -> BusinessException.unauthorized("用户不存在"));
        String newAccess = jwtUtil.generateAccessToken(user.getEmail(), user.getRole().name());
        return new TokenResponse(newAccess, refreshToken, user.getRole().name(),
                user.getId(), user.getUsername());
    }

    private TokenResponse buildTokenResponse(User user) {
        String access = jwtUtil.generateAccessToken(user.getEmail(), user.getRole().name());
        String refresh = jwtUtil.generateRefreshToken(user.getEmail());
        return new TokenResponse(access, refresh, user.getRole().name(),
                user.getId(), user.getUsername());
    }
}
