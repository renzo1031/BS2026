package com.campusbuddies.governance;

import com.campusbuddies.common.ApiResponse;
import com.campusbuddies.common.PageResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class GovernanceController {
    public record SubmitInput(@NotNull ReportTargetType targetType, long targetId,
                              @NotBlank @Size(max = 40) String reasonCode,
                              @Size(max = 1000) String description) {}
    public record VersionInput(@Min(0) int version) {}
    public record DecisionInput(@Min(0) int version, boolean actioned,
                                ModerationActionType actionType,
                                @NotBlank @Size(max = 1000) String resolution,
                                @Min(1) @Max(720) Integer durationHours) {}
    public record AppealInput(@Min(0) int version, @NotBlank @Size(max = 1000) String reason) {}
    public record AppealDecisionInput(@Min(0) int version, boolean uphold,
                                      @NotBlank @Size(max = 1000) String resolution) {}

    private final GovernanceService service;

    public GovernanceController(GovernanceService service) { this.service = service; }

    @PostMapping("/reports")
    public ApiResponse<GovernanceService.ReportView> submit(@Valid @RequestBody SubmitInput input) {
        return ApiResponse.ok(service.submit(input.targetType(), input.targetId(), input.reasonCode(), input.description()));
    }

    @GetMapping("/reports/mine")
    public ApiResponse<PageResult<GovernanceService.ReportView>> mine(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(service.mine(page, size));
    }

    @GetMapping("/reports/affected")
    public ApiResponse<PageResult<GovernanceService.ReportView>> affected(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(service.affected(page, size));
    }

    @PostMapping("/reports/{id}/appeal")
    public ApiResponse<GovernanceService.ReportView> appeal(@PathVariable long id,
                                                             @Valid @RequestBody AppealInput input) {
        return ApiResponse.ok(service.appeal(id, input.version(), input.reason()));
    }

    @GetMapping("/review/reports")
    public ApiResponse<PageResult<GovernanceService.ReviewView>> reviewQueue(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(service.reviewQueue(page, size));
    }

    @PostMapping("/review/reports/{id}/claim")
    public ApiResponse<GovernanceService.ReviewView> claim(@PathVariable long id,
                                                            @Valid @RequestBody VersionInput input) {
        return ApiResponse.ok(service.claim(id, input.version()));
    }

    @PostMapping("/review/reports/{id}/decision")
    public ApiResponse<GovernanceService.ReviewView> decide(@PathVariable long id,
                                                             @Valid @RequestBody DecisionInput input) {
        return ApiResponse.ok(service.decide(id, input.version(), input.actioned(), input.actionType(),
                input.resolution(), input.durationHours()));
    }

    @PostMapping("/admin/reports/{id}/appeal-decision")
    public ApiResponse<GovernanceService.ReviewView> resolveAppeal(@PathVariable long id,
                                                                   @Valid @RequestBody AppealDecisionInput input) {
        return ApiResponse.ok(service.resolveAppeal(id, input.version(), input.uphold(), input.resolution()));
    }

    @PostMapping("/blocks/{blockedId}")
    public ApiResponse<Void> block(@PathVariable long blockedId) {
        service.block(blockedId);
        return ApiResponse.ok();
    }

    @DeleteMapping("/blocks/{blockedId}")
    public ApiResponse<Void> unblock(@PathVariable long blockedId) {
        service.unblock(blockedId);
        return ApiResponse.ok();
    }

    @GetMapping("/blocks")
    public ApiResponse<List<GovernanceService.BlockedUserView>> blockedUsers() {
        return ApiResponse.ok(service.blockedUsers());
    }

    @GetMapping("/admin/audit-logs")
    public ApiResponse<PageResult<GovernanceService.AuditView>> auditLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String actionName) {
        return ApiResponse.ok(service.auditLogs(page, size, actionName));
    }
}
