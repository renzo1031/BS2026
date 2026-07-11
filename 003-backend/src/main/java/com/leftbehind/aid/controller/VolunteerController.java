package com.leftbehind.aid.controller;

import com.leftbehind.aid.api.ApiModels;
import com.leftbehind.aid.common.ApiResponse;
import com.leftbehind.aid.service.VolunteerService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/volunteers/me")
@PreAuthorize("hasRole('VOLUNTEER') and hasAuthority('volunteer:profile')")
public class VolunteerController {
    private final VolunteerService volunteerService;

    public VolunteerController(VolunteerService volunteerService) {
        this.volunteerService = volunteerService;
    }

    @GetMapping
    public ApiResponse<ApiModels.VolunteerView> me() {
        return ApiResponse.success(volunteerService.me());
    }

    @PutMapping
    public ApiResponse<ApiModels.VolunteerView> update(@Valid @RequestBody ApiModels.VolunteerProfileRequest request) {
        return ApiResponse.success(volunteerService.update(request));
    }

    @PostMapping("/submit")
    public ApiResponse<Void> submit() {
        volunteerService.submit();
        return ApiResponse.success();
    }
}
