package com.leftbehind.aid.controller;

import com.leftbehind.aid.api.ApiModels;
import com.leftbehind.aid.common.ApiResponse;
import com.leftbehind.aid.common.PageResult;
import com.leftbehind.aid.service.AidService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/aid-requests")
public class AidController {
    private final AidService aidService;

    public AidController(AidService aidService) {
        this.aidService = aidService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SYS_ADMIN','SUPERVISOR','CASE_WORKER') and hasAuthority('aid:read')")
    public ApiResponse<PageResult<ApiModels.AidView>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.success(aidService.list(page, size, status, category, keyword));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYS_ADMIN','SUPERVISOR','CASE_WORKER') and hasAuthority('aid:read')")
    public ApiResponse<ApiModels.AidView> detail(@PathVariable Long id) {
        return ApiResponse.success(aidService.detail(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('aid:write')")
    public ApiResponse<ApiModels.AidView> create(@Valid @RequestBody ApiModels.AidUpsertRequest request) {
        return ApiResponse.success(aidService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('aid:write')")
    public ApiResponse<ApiModels.AidView> update(@PathVariable Long id,
                                                 @Valid @RequestBody ApiModels.AidUpsertRequest request) {
        return ApiResponse.success(aidService.update(id, request));
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAuthority('aid:write')")
    public ApiResponse<Void> submit(@PathVariable Long id, @Valid @RequestBody ApiModels.VersionRequest request) {
        aidService.submit(id, request.version());
        return ApiResponse.success();
    }

    @PostMapping("/{id}/review")
    @PreAuthorize("hasAuthority('aid:review')")
    public ApiResponse<Void> review(@PathVariable Long id, @Valid @RequestBody ApiModels.ReviewRequest request) {
        aidService.review(id, request);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyAuthority('aid:write','aid:review')")
    public ApiResponse<Void> cancel(@PathVariable Long id, @Valid @RequestBody ApiModels.CancelRequest request) {
        aidService.cancel(id, request);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/applications")
    @PreAuthorize("hasRole('VOLUNTEER') and hasAuthority('application:apply')")
    public ApiResponse<Void> apply(@PathVariable Long id,
                                   @Valid @RequestBody ApiModels.ApplicationCreateRequest request) {
        aidService.apply(id, request);
        return ApiResponse.success();
    }

    @GetMapping("/{id}/applications")
    @PreAuthorize("hasAuthority('application:manage')")
    public ApiResponse<List<ApiModels.ApplicationView>> applications(@PathVariable Long id) {
        return ApiResponse.success(aidService.applicationsForRequest(id));
    }
}
