package com.campusbuddies.activity;

import com.campusbuddies.common.ApiResponse;
import com.campusbuddies.common.PageResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/activities")
public class ActivityController {
    public record ActivityInput(
            @NotBlank @Size(min = 2, max = 20) String sceneName,
            @NotBlank @Size(min = 5, max = 50) String title,
            @NotBlank @Size(min = 20, max = 1000) String description,
            @NotNull MeetingMode meetingMode,
            @Size(max = 120) String publicLocation,
            @Size(max = 255) String memberLocationDetail,
            @Size(max = 500) String joinRequirement,
            @Size(max = 3) List<@Size(min = 2, max = 100) String> joinQuestions,
            @NotNull Instant startAt,
            @NotNull Instant endAt,
            @NotNull Instant applyDeadline,
            @NotNull @Min(2) @Max(50) Integer capacity,
            @Size(max = 5) List<@Size(min = 2, max = 12) String> tags) {
        ActivityService.Command command() {
            return new ActivityService.Command(sceneName, title, description, meetingMode, publicLocation,
                    memberLocationDetail, joinRequirement, joinQuestions, startAt, endAt, applyDeadline,
                    capacity, tags);
        }
    }

    public record VersionInput(@Min(0) int version) {}
    public record UpdateInput(@Min(0) int version, @Valid @NotNull ActivityInput activity) {}
    public record CancelInput(@Min(0) int version, @Size(max = 500) String reason) {}
    public record CompletionInput(boolean disputed) {}

    private final ActivityService service;

    public ActivityController(ActivityService service) { this.service = service; }

    @PostMapping
    public ApiResponse<ActivityService.ActivityView> create(@Valid @RequestBody ActivityInput input) {
        return ApiResponse.ok(service.create(input.command()));
    }

    @PutMapping("/{id}")
    public ApiResponse<ActivityService.ActivityView> update(@PathVariable long id,
                                                             @Valid @RequestBody UpdateInput input) {
        return ApiResponse.ok(service.update(id, input.version(), input.activity().command()));
    }

    @PostMapping("/{id}/submit")
    public ApiResponse<ActivityService.ActivityView> submit(@PathVariable long id,
                                                             @Valid @RequestBody VersionInput input) {
        return ApiResponse.ok(service.submit(id, input.version()));
    }

    @PostMapping("/{id}/start")
    public ApiResponse<ActivityService.ActivityView> start(@PathVariable long id,
                                                            @Valid @RequestBody VersionInput input) {
        return ApiResponse.ok(service.start(id, input.version()));
    }

    @PostMapping("/{id}/request-completion")
    public ApiResponse<ActivityService.ActivityView> requestCompletion(@PathVariable long id,
                                                                        @Valid @RequestBody VersionInput input) {
        return ApiResponse.ok(service.requestCompletion(id, input.version()));
    }

    @PostMapping("/{id}/completion-confirmation")
    public ApiResponse<ActivityService.ActivityView> confirmCompletion(@PathVariable long id,
                                                                        @RequestBody CompletionInput input) {
        return ApiResponse.ok(service.confirmCompletion(id, input.disputed()));
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<ActivityService.ActivityView> cancel(@PathVariable long id,
                                                             @Valid @RequestBody CancelInput input) {
        return ApiResponse.ok(service.cancel(id, input.version(), input.reason()));
    }

    @GetMapping
    public ApiResponse<PageResult<ActivityService.ActivityView>> discover(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) MeetingMode mode) {
        return ApiResponse.ok(service.discover(page, size, keyword, mode));
    }

    @GetMapping("/mine")
    public ApiResponse<PageResult<ActivityService.ActivityView>> mine(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(service.mine(page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<ActivityService.ActivityView> detail(@PathVariable long id) {
        return ApiResponse.ok(service.detail(id));
    }
}
