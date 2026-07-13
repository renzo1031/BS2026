package com.campusbuddies.activity;

import com.campusbuddies.common.ApiResponse;
import com.campusbuddies.common.PageResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class ApplicationController {
    public record ApplyInput(
            @Size(max = 3) List<@Size(max = 300) String> answers,
            @Size(max = 500) String message) {}
    public record DecisionInput(@Min(0) int version, @NotNull Boolean accept, @Size(max = 500) String reason) {}
    public record VersionInput(@Min(0) int version) {}

    private final ApplicationService service;

    public ApplicationController(ApplicationService service) { this.service = service; }

    @PostMapping("/activities/{activityId}/applications")
    public ApiResponse<ApplicationService.ApplicationView> apply(@PathVariable long activityId,
                                                                  @Valid @RequestBody ApplyInput input) {
        return ApiResponse.ok(service.apply(activityId, input.answers(), input.message()));
    }

    @GetMapping("/activities/{activityId}/applications")
    public ApiResponse<PageResult<ApplicationService.ApplicationView>> forActivity(
            @PathVariable long activityId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(service.forActivity(activityId, page, size));
    }

    @GetMapping("/applications/mine")
    public ApiResponse<PageResult<ApplicationService.ApplicationView>> mine(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(service.mine(page, size));
    }

    @PostMapping("/applications/{id}/decision")
    public ApiResponse<ApplicationService.ApplicationView> decide(@PathVariable long id,
                                                                   @Valid @RequestBody DecisionInput input) {
        return ApiResponse.ok(service.decide(id, input.version(), input.accept(), input.reason()));
    }

    @PostMapping("/applications/{id}/withdraw")
    public ApiResponse<ApplicationService.ApplicationView> withdraw(@PathVariable long id,
                                                                     @Valid @RequestBody VersionInput input) {
        return ApiResponse.ok(service.withdraw(id, input.version()));
    }
}
