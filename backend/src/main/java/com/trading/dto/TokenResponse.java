package com.trading.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data @AllArgsConstructor
public class TokenResponse {
    private String accessToken;
    private String refreshToken;
    private String role;
    private Long userId;
    private String username;
}
