package com.leftbehind.aid.controller;

import com.leftbehind.aid.api.ApiModels;
import com.leftbehind.aid.common.ApiResponse;
import com.leftbehind.aid.service.AidService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/applications")
public class ApplicationController {
    private final AidService aidService;

    public ApplicationController(AidService aidService) {
        this.aidService = aidService;
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('VOLUNTEER') and hasAuthority('application:read')")
    public ApiResponse<List<ApiModels.ApplicationView>> mine() {
        return ApiResponse.success(aidService.myApplications());
    }

    @PostMapping("/{id}/withdraw")
    @PreAuthorize("hasRole('VOLUNTEER') and hasAuthority('application:apply')")
    public ApiResponse<Void> withdraw(@PathVariable Long id) {
        aidService.withdrawApplication(id);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/accept")
    @PreAuthorize("hasAuthority('application:manage')")
    public ApiResponse<Long> accept(@PathVariable Long id,
                                    @Valid @RequestBody ApiModels.AcceptApplicationRequest request) {
        return ApiResponse.success(aidService.acceptApplication(id, request.requestVersion()));
    }
}
