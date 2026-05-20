package com.trading.dto;

import com.trading.entity.User;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminUserResponse {
    private Long id;
    private String username;
    private String email;
    private String role;
    private String status;
    private LocalDateTime createdAt;

    public static AdminUserResponse from(User u) {
        AdminUserResponse r = new AdminUserResponse();
        r.id = u.getId();
        r.username = u.getUsername();
        r.email = u.getEmail();
        r.role = u.getRole().name();
        r.status = u.getStatus().name();
        r.createdAt = u.getCreatedAt();
        return r;
    }
}
