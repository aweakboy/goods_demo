package com.trading.controller;

import com.trading.common.ApiResponse;
import com.trading.dto.MembershipPlanRequest;
import com.trading.dto.MembershipPlanResponse;
import com.trading.enums.MembershipPlanStatus;
import com.trading.service.MembershipAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/membership/plans")
@RequiredArgsConstructor
public class AdminMembershipController {

    private final MembershipAdminService membershipAdminService;

    @GetMapping
    public ApiResponse<Page<MembershipPlanResponse>> list(
            @RequestParam(required = false) MembershipPlanStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(membershipAdminService.list(status, page, size).map(MembershipPlanResponse::from));
    }

    @PostMapping
    public ApiResponse<MembershipPlanResponse> create(@Valid @RequestBody MembershipPlanRequest request) {
        return ApiResponse.created(MembershipPlanResponse.from(membershipAdminService.create(request)));
    }

    @PutMapping("/{id}")
    public ApiResponse<MembershipPlanResponse> update(@PathVariable Long id,
                                                      @Valid @RequestBody MembershipPlanRequest request) {
        return ApiResponse.ok(MembershipPlanResponse.from(membershipAdminService.update(id, request)));
    }

    @PostMapping("/{id}/activate")
    public ApiResponse<MembershipPlanResponse> activate(@PathVariable Long id) {
        return ApiResponse.ok(MembershipPlanResponse.from(membershipAdminService.activate(id)));
    }

    @PostMapping("/{id}/deactivate")
    public ApiResponse<MembershipPlanResponse> deactivate(@PathVariable Long id) {
        return ApiResponse.ok(MembershipPlanResponse.from(membershipAdminService.deactivate(id)));
    }
}
