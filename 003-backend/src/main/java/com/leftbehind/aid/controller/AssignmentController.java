package com.leftbehind.aid.controller;

import com.leftbehind.aid.api.ApiModels;
import com.leftbehind.aid.common.ApiResponse;
import com.leftbehind.aid.common.PageResult;
import com.leftbehind.aid.service.AssignmentService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/assignments")
public class AssignmentController {
    private final AssignmentService assignmentService;

    public AssignmentController(AssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('assignment:read')")
    public ApiResponse<PageResult<ApiModels.AssignmentView>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {
        return ApiResponse.success(assignmentService.list(page, size, status));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('assignment:read')")
    public ApiResponse<ApiModels.AssignmentView> detail(@PathVariable Long id) {
        return ApiResponse.success(assignmentService.detail(id));
    }

    @PostMapping("/{id}/start")
    @PreAuthorize("hasAuthority('assignment:execute')")
    public ApiResponse<Void> start(@PathVariable Long id, @Valid @RequestBody ApiModels.VersionRequest request) {
        assignmentService.start(id, request.version());
        return ApiResponse.success();
    }

    @PostMapping("/{id}/visits")
    @PreAuthorize("hasAuthority('assignment:execute')")
    public ApiResponse<Void> addVisit(@PathVariable Long id,
                                      @Valid @RequestBody ApiModels.VisitCreateRequest request) {
        assignmentService.addVisit(id, request);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/submit-completion")
    @PreAuthorize("hasAuthority('assignment:execute')")
    public ApiResponse<Void> submitCompletion(@PathVariable Long id,
                                              @Valid @RequestBody ApiModels.CompletionRequest request) {
        assignmentService.submitCompletion(id, request);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasAuthority('assignment:confirm')")
    public ApiResponse<Void> confirm(@PathVariable Long id, @Valid @RequestBody ApiModels.VersionRequest request) {
        assignmentService.confirm(id, request.version());
        return ApiResponse.success();
    }

    @PostMapping("/{id}/feedback")
    @PreAuthorize("hasAuthority('feedback:write')")
    public ApiResponse<Void> feedback(@PathVariable Long id,
                                      @Valid @RequestBody ApiModels.FeedbackRequest request) {
        assignmentService.feedback(id, request);
        return ApiResponse.success();
    }
}
