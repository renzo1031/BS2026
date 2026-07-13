package com.campusbuddies.admin;

import com.campusbuddies.common.ApiResponse;
import com.campusbuddies.common.PageResult;
import com.campusbuddies.user.UserRole;
import com.campusbuddies.user.UserStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
public class PlatformAdminController {
    private final PlatformAdminService service;

    public PlatformAdminController(PlatformAdminService service) { this.service = service; }

    public record UserUpdate(@NotBlank @Size(min = 1, max = 40) String nickname,
                             Long campusId, UserRole role, UserStatus status) {}

    public record CampusUpdate(@NotBlank @Size(min = 2, max = 80) String name,
                               @NotBlank @Size(min = 2, max = 32) String code,
                               @NotBlank @Size(max = 20) String status,
                               @NotBlank @Size(min = 1, max = 40) String identityLabel) {}

    public record TagCreate(@NotNull Long campusId,
                            @NotBlank @Size(min = 2, max = 12) String name,
                            @NotBlank @Size(max = 20) String status) {}

    public record TagUpdate(@NotBlank @Size(min = 2, max = 12) String name,
                            @NotBlank @Size(max = 20) String status) {}

    @GetMapping("/users")
    public ApiResponse<PageResult<PlatformAdminService.UserView>> users(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) Long campusId) {
        return ApiResponse.ok(service.users(page, size, keyword, role, status, campusId));
    }

    @PutMapping("/users/{id}")
    public ApiResponse<PlatformAdminService.UserView> updateUser(@PathVariable long id, @Valid @RequestBody UserUpdate input) {
        return ApiResponse.ok(service.updateUser(id, new PlatformAdminService.UserCommand(
                input.nickname(), input.campusId(), input.role(), input.status())));
    }

    @GetMapping("/campuses")
    public ApiResponse<PageResult<PlatformAdminService.CampusView>> campuses(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        return ApiResponse.ok(service.campuses(page, size, keyword, status));
    }

    @PostMapping("/campuses")
    public ApiResponse<PlatformAdminService.CampusView> createCampus(@Valid @RequestBody CampusUpdate input) {
        return ApiResponse.ok(service.createCampus(new PlatformAdminService.CampusCommand(
                input.name(), input.code(), input.status(), input.identityLabel())));
    }

    @PutMapping("/campuses/{id}")
    public ApiResponse<PlatformAdminService.CampusView> updateCampus(@PathVariable long id, @Valid @RequestBody CampusUpdate input) {
        return ApiResponse.ok(service.updateCampus(id, new PlatformAdminService.CampusCommand(
                input.name(), input.code(), input.status(), input.identityLabel())));
    }

    @GetMapping("/tags")
    public ApiResponse<PageResult<PlatformAdminService.TagView>> tags(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) int size,
            @RequestParam(required = false) Long campusId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        return ApiResponse.ok(service.tags(page, size, campusId, keyword, status));
    }

    @PostMapping("/tags")
    public ApiResponse<PlatformAdminService.TagView> createTag(@Valid @RequestBody TagCreate input) {
        return ApiResponse.ok(service.createTag(new PlatformAdminService.TagCreateCommand(
                input.campusId(), input.name(), input.status())));
    }

    @PutMapping("/tags/{id}")
    public ApiResponse<PlatformAdminService.TagView> updateTag(@PathVariable long id, @Valid @RequestBody TagUpdate input) {
        return ApiResponse.ok(service.updateTag(id, new PlatformAdminService.TagUpdateCommand(
                input.name(), input.status())));
    }
}
