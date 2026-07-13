package com.campusbuddies.activity;

import com.campusbuddies.common.ApiResponse;
import com.campusbuddies.common.PageResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/operations/activities")
public class ActivityOperationsController {
    private final ActivityService service;
    private final ActivityReviewDetailService detailService;

    public ActivityOperationsController(ActivityService service, ActivityReviewDetailService detailService) {
        this.service = service;
        this.detailService = detailService;
    }

    @GetMapping
    public ApiResponse<PageResult<ActivityService.ActivityView>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) ActivityReviewStatus reviewStatus,
            @RequestParam(required = false) ActivityLifecycleStatus lifecycleStatus,
            @RequestParam(required = false) ActivityModerationStatus moderationStatus,
            @RequestParam(required = false) MeetingMode meetingMode) {
        return ApiResponse.ok(service.operations(page, size, keyword, reviewStatus, lifecycleStatus,
                moderationStatus, meetingMode));
    }

    @GetMapping("/{id}")
    public ApiResponse<ActivityReviewDetailService.DetailView> detail(@PathVariable long id) {
        return ApiResponse.ok(detailService.detail(id));
    }
}
