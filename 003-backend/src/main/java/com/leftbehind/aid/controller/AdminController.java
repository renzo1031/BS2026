package com.leftbehind.aid.controller;

import com.leftbehind.aid.api.ApiModels;
import com.leftbehind.aid.common.ApiResponse;
import com.leftbehind.aid.common.PageResult;
import com.leftbehind.aid.domain.Domain;
import com.leftbehind.aid.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/users")
    @PreAuthorize("hasAuthority('system:user:manage')")
    public ApiResponse<PageResult<ApiModels.UserAdminView>> users(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String roleCode) {
        return ApiResponse.success(adminService.users(page, size, keyword, roleCode));
    }

    @PostMapping("/users")
    @PreAuthorize("hasAuthority('system:user:manage')")
    public ApiResponse<Void> createUser(@Valid @RequestBody ApiModels.AdminCreateUserRequest request) {
        adminService.createUser(request);
        return ApiResponse.success();
    }

    @PatchMapping("/users/{id}/status")
    @PreAuthorize("hasAuthority('system:user:manage')")
    public ApiResponse<Void> updateUserStatus(@PathVariable Long id,
                                              @Valid @RequestBody ApiModels.UserStatusRequest request) {
        adminService.updateUserStatus(id, request.status());
        return ApiResponse.success();
    }

    @GetMapping("/departments")
    @PreAuthorize("hasAuthority('system:user:manage')")
    public ApiResponse<List<Domain.DepartmentRow>> departments() {
        return ApiResponse.success(adminService.departments());
    }

    @PostMapping("/departments")
    @PreAuthorize("hasAuthority('system:user:manage')")
    public ApiResponse<Void> createDepartment(@Valid @RequestBody ApiModels.DepartmentRequest request) {
        adminService.createDepartment(request);
        return ApiResponse.success();
    }

    @GetMapping("/roles")
    @PreAuthorize("hasAuthority('system:role:manage')")
    public ApiResponse<List<ApiModels.RoleView>> roles() {
        return ApiResponse.success(adminService.roles());
    }

    @GetMapping("/permissions")
    @PreAuthorize("hasAuthority('system:role:manage')")
    public ApiResponse<List<Domain.PermissionRow>> permissions() {
        return ApiResponse.success(adminService.permissions());
    }

    @PutMapping("/roles/{id}/permissions")
    @PreAuthorize("hasAuthority('system:role:manage')")
    public ApiResponse<Void> updateRolePermissions(@PathVariable Long id,
                                                   @Valid @RequestBody ApiModels.RolePermissionRequest request) {
        adminService.updateRolePermissions(id, request);
        return ApiResponse.success();
    }

    @GetMapping("/volunteers")
    @PreAuthorize("hasAuthority('volunteer:review')")
    public ApiResponse<PageResult<ApiModels.VolunteerView>> volunteers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {
        return ApiResponse.success(adminService.volunteers(page, size, status));
    }

    @PostMapping("/volunteers/{id}/review")
    @PreAuthorize("hasAuthority('volunteer:review')")
    public ApiResponse<Void> reviewVolunteer(@PathVariable Long id,
                                             @Valid @RequestBody ApiModels.DecisionRequest request) {
        adminService.reviewVolunteer(id, request);
        return ApiResponse.success();
    }

    @GetMapping("/audit-logs")
    @PreAuthorize("hasAuthority('system:audit:read')")
    public ApiResponse<PageResult<Domain.AuditRow>> auditLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String businessType) {
        return ApiResponse.success(adminService.auditLogs(page, size, businessType));
    }
}
