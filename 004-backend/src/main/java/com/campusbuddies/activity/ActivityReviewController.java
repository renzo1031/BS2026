package com.campusbuddies.activity;

import com.campusbuddies.common.ApiResponse;
import com.campusbuddies.common.PageResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/review/activities")
public class ActivityReviewController {
    public record ClaimInput(@Min(0) int version) {}
    public record DecisionInput(@Min(0) int version, @NotNull Boolean approve, @Size(max = 500) String reason) {}

    private final ActivityService service;
    private final ActivityReviewDetailService detailService;

    public ActivityReviewController(ActivityService service, ActivityReviewDetailService detailService) {
        this.service = service;
        this.detailService = detailService;
    }

    @GetMapping
    public ApiResponse<PageResult<ActivityService.ActivityView>> queue(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(service.reviewQueue(page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<ActivityReviewDetailService.DetailView> detail(@PathVariable long id) {
        return ApiResponse.ok(detailService.detail(id));
    }

    @PostMapping("/{id}/claim")
    public ApiResponse<ActivityService.ActivityView> claim(@PathVariable long id,
                                                            @Valid @RequestBody ClaimInput input) {
        return ApiResponse.ok(service.claim(id, input.version()));
    }

    @PostMapping("/{id}/decision")
    public ApiResponse<ActivityService.ActivityView> decide(@PathVariable long id,
                                                             @Valid @RequestBody DecisionInput input) {
        return ApiResponse.ok(service.decide(id, input.version(), input.approve(), input.reason()));
    }
}
