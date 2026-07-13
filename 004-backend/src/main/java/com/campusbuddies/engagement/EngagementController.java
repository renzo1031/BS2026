package com.campusbuddies.engagement;

import com.campusbuddies.common.ApiResponse;
import com.campusbuddies.common.PageResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class EngagementController {
    public record EvaluationInput(@Min(1) @Max(5) int rating,
                                  @Size(max = 5) List<@Size(max = 20) String> tags,
                                  @Size(max = 500) String privateNote) {}

    private final EngagementService service;

    public EngagementController(EngagementService service) { this.service = service; }

    @PostMapping("/activities/{activityId}/favorite")
    public ApiResponse<Void> favorite(@PathVariable long activityId) {
        service.favorite(activityId);
        return ApiResponse.ok();
    }

    @DeleteMapping("/activities/{activityId}/favorite")
    public ApiResponse<Void> unfavorite(@PathVariable long activityId) {
        service.unfavorite(activityId);
        return ApiResponse.ok();
    }

    @GetMapping("/favorites")
    public ApiResponse<PageResult<EngagementService.FavoriteView>> favorites(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(service.favorites(page, size));
    }

    @PostMapping("/activities/{activityId}/evaluations/{revieweeId}")
    public ApiResponse<EngagementService.EvaluationView> evaluate(@PathVariable long activityId,
                                                                   @PathVariable long revieweeId,
                                                                   @Valid @RequestBody EvaluationInput input) {
        return ApiResponse.ok(service.evaluate(activityId, revieweeId, input.rating(), input.tags(), input.privateNote()));
    }

    @GetMapping("/activities/{activityId}/evaluation-targets")
    public ApiResponse<List<EngagementService.EvaluationTarget>> targets(@PathVariable long activityId) {
        return ApiResponse.ok(service.targets(activityId));
    }

    @GetMapping("/me/reputation")
    public ApiResponse<EngagementService.ReputationView> reputation() {
        return ApiResponse.ok(service.reputation());
    }
}
