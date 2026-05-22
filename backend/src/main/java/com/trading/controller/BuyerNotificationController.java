package com.trading.controller;

import com.trading.common.ApiResponse;
import com.trading.dto.BuyerNotificationResponse;
import com.trading.dto.UnreadCountResponse;
import com.trading.entity.User;
import com.trading.service.BuyerNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/buyer/notifications")
@RequiredArgsConstructor
public class BuyerNotificationController {

    private final BuyerNotificationService buyerNotificationService;

    @GetMapping
    public ApiResponse<Page<BuyerNotificationResponse>> mine(@AuthenticationPrincipal User user,
                                                             @RequestParam(defaultValue = "0") int page,
                                                             @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(buyerNotificationService.listNotifications(user.getId(), page, size));
    }

    @GetMapping("/unread-count")
    public ApiResponse<UnreadCountResponse> unreadCount(@AuthenticationPrincipal User user) {
        return ApiResponse.ok(new UnreadCountResponse(buyerNotificationService.countUnread(user.getId())));
    }

    @PostMapping("/{id}/read")
    public ApiResponse<BuyerNotificationResponse> markRead(@AuthenticationPrincipal User user,
                                                           @PathVariable Long id) {
        return ApiResponse.ok(buyerNotificationService.markRead(user.getId(), id));
    }

    @PostMapping("/read-all")
    public ApiResponse<UnreadCountResponse> markAllRead(@AuthenticationPrincipal User user) {
        buyerNotificationService.markAllRead(user.getId());
        return ApiResponse.ok(new UnreadCountResponse(0));
    }
}
