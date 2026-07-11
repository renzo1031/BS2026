package com.leftbehind.aid.controller;

import com.leftbehind.aid.api.ApiModels;
import com.leftbehind.aid.common.ApiResponse;
import com.leftbehind.aid.common.PageResult;
import com.leftbehind.aid.service.ChildService;
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

@RestController
@RequestMapping("/api/children")
public class ChildController {
    private final ChildService childService;

    public ChildController(ChildService childService) {
        this.childService = childService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('child:read')")
    public ApiResponse<PageResult<ApiModels.ChildView>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.success(childService.list(page, size, status, keyword));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('child:read')")
    public ApiResponse<ApiModels.ChildView> detail(@PathVariable Long id) {
        return ApiResponse.success(childService.detail(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('child:write')")
    public ApiResponse<ApiModels.ChildView> create(@Valid @RequestBody ApiModels.ChildUpsertRequest request) {
        return ApiResponse.success(childService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('child:write')")
    public ApiResponse<ApiModels.ChildView> update(@PathVariable Long id,
                                                   @Valid @RequestBody ApiModels.ChildUpsertRequest request) {
        return ApiResponse.success(childService.update(id, request));
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAuthority('child:write')")
    public ApiResponse<Void> submit(@PathVariable Long id, @Valid @RequestBody ApiModels.VersionRequest request) {
        childService.submit(id, request.version());
        return ApiResponse.success();
    }

    @PostMapping("/{id}/review")
    @PreAuthorize("hasAuthority('child:review')")
    public ApiResponse<Void> review(@PathVariable Long id, @Valid @RequestBody ApiModels.ReviewRequest request) {
        childService.review(id, request);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/archive")
    @PreAuthorize("hasAuthority('child:archive')")
    public ApiResponse<Void> archive(@PathVariable Long id, @Valid @RequestBody ApiModels.VersionRequest request) {
        childService.archive(id, request.version());
        return ApiResponse.success();
    }
}
