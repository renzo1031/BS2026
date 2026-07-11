package com.leftbehind.aid.controller;

import com.leftbehind.aid.api.ApiModels;
import com.leftbehind.aid.common.ApiResponse;
import com.leftbehind.aid.common.PageResult;
import com.leftbehind.aid.domain.Domain;
import com.leftbehind.aid.mapper.SystemMapper;
import com.leftbehind.aid.service.AidService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public")
public class PublicController {
    private final AidService aidService;
    private final SystemMapper systemMapper;

    public PublicController(AidService aidService, SystemMapper systemMapper) {
        this.aidService = aidService;
        this.systemMapper = systemMapper;
    }

    @GetMapping("/overview")
    public ApiResponse<ApiModels.PublicOverview> overview() {
        Domain.PublicStats stats = systemMapper.publicStats();
        return ApiResponse.success(new ApiModels.PublicOverview(stats.completedServices(), stats.approvedVolunteers(),
                stats.activeRequests(), stats.serviceDepartments()));
    }

    @GetMapping("/aid-requests")
    public ApiResponse<PageResult<ApiModels.PublicAidView>> aidRequests(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "9") int size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.success(aidService.publicList(page, size, category, keyword));
    }

    @GetMapping("/aid-requests/{id}")
    public ApiResponse<ApiModels.PublicAidView> aidRequest(@PathVariable Long id) {
        return ApiResponse.success(aidService.publicDetail(id));
    }
}
