package com.campus.service.controller;

import com.campus.service.common.ApiResult;
import com.campus.service.common.PageResult;
import com.campus.service.common.RequireRole;
import com.campus.service.entity.*;
import com.campus.service.service.AdminService;
import com.campus.service.service.RequestService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/admin")
@RequireRole({"ADMIN", "STAFF"})
public class AdminController {
    private final RequestService requestService;
    private final AdminService adminService;

    public AdminController(RequestService requestService, AdminService adminService) {
        this.requestService = requestService;
        this.adminService = adminService;
    }

    @GetMapping("/requests")
    public ApiResult<PageResult<RequestService.RequestListItem>> requests(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) long size) {
        return ApiResult.ok(requestService.todo(status, page, size));
    }

    @PostMapping("/requests/{id}/accept")
    public ApiResult<Void> accept(@PathVariable Long id) {
        requestService.accept(id);
        return ApiResult.ok();
    }

    @PostMapping("/requests/{id}/approve")
    public ApiResult<Void> approve(@PathVariable Long id, @Valid @RequestBody ApproveRequest request) {
        requestService.approve(id, new RequestService.ApproveCommand(request.approved(), request.comment()));
        return ApiResult.ok();
    }

    @PostMapping("/requests/{id}/finish")
    public ApiResult<Void> finish(@PathVariable Long id, @Valid @RequestBody FinishRequest request) {
        requestService.finish(id, new RequestService.FinishCommand(request.result()));
        return ApiResult.ok();
    }

    @GetMapping("/stats")
    public ApiResult<RequestService.Stats> stats() {
        return ApiResult.ok(requestService.stats());
    }

    @GetMapping("/users")
    @RequireRole("ADMIN")
    public ApiResult<PageResult<AdminService.UserSummary>> users(
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) long size,
            @RequestParam(required = false) @Size(max = 80) String keyword) {
        return ApiResult.ok(adminService.users(page, size, keyword));
    }

    @PostMapping("/staff")
    @RequireRole("ADMIN")
    public ApiResult<AdminService.UserSummary> createStaff(@Valid @RequestBody CreateStaffRequest request) {
        return ApiResult.ok(adminService.createStaff(new AdminService.CreateStaffCommand(
                request.username(), request.password(), request.realName(), request.departmentId(),
                request.phone(), request.email())));
    }

    @PatchMapping("/users/{id}/status")
    @RequireRole("ADMIN")
    public ApiResult<Void> updateUserStatus(@PathVariable Long id, @Valid @RequestBody UserStatusRequest request) {
        adminService.updateUserStatus(id, request.status());
        return ApiResult.ok();
    }

    @GetMapping("/departments")
    @RequireRole("ADMIN")
    public ApiResult<List<Department>> departments() {
        return ApiResult.ok(adminService.departments());
    }

    @GetMapping("/service-items")
    @RequireRole("ADMIN")
    public ApiResult<List<AdminService.ServiceItemView>> serviceItems() {
        return ApiResult.ok(adminService.serviceItems());
    }

    @PatchMapping("/service-items/{id}/enabled")
    @RequireRole("ADMIN")
    public ApiResult<Void> updateServiceItem(@PathVariable Long id, @Valid @RequestBody EnabledRequest request) {
        adminService.updateServiceItemEnabled(id, request.enabled());
        return ApiResult.ok();
    }

    @GetMapping("/venues")
    @RequireRole("ADMIN")
    public ApiResult<List<Venue>> venues() {
        return ApiResult.ok(adminService.venues());
    }

    @PatchMapping("/venues/{id}/status")
    @RequireRole("ADMIN")
    public ApiResult<Void> updateVenue(@PathVariable Long id, @Valid @RequestBody VenueStatusRequest request) {
        adminService.updateVenueStatus(id, request.status());
        return ApiResult.ok();
    }

    @GetMapping("/logs")
    @RequireRole("ADMIN")
    public ApiResult<PageResult<OperationLog>> logs(
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) long size) {
        return ApiResult.ok(adminService.logs(page, size));
    }

    public record ApproveRequest(@NotNull Boolean approved, @NotBlank @Size(max = 500) String comment) {
    }

    public record FinishRequest(@NotBlank @Size(max = 500) String result) {
    }

    public record CreateStaffRequest(
            @NotBlank @Size(min = 4, max = 30) @Pattern(regexp = "[A-Za-z0-9_]+") String username,
            @NotBlank @Size(min = 8, max = 64) String password,
            @NotBlank @Size(max = 50) String realName,
            @NotNull Long departmentId,
            @Pattern(regexp = "^$|^1\\d{10}$") String phone,
            @Email @Size(max = 100) String email) {
    }

    public record UserStatusRequest(@NotBlank @Pattern(regexp = "ENABLED|DISABLED") String status) {
    }

    public record EnabledRequest(@NotNull Boolean enabled) {
    }

    public record VenueStatusRequest(@NotBlank @Pattern(regexp = "AVAILABLE|UNAVAILABLE") String status) {
    }
}
