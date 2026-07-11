package com.leftbehind.aid.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public final class ApiModels {
    private ApiModels() {
    }

    public record LoginRequest(
            @NotBlank @Size(max = 50) String username,
            @NotBlank @Size(max = 64) String password
    ) {
    }

    public record RegisterRequest(
            @NotBlank @Pattern(regexp = "[A-Za-z0-9_]{4,32}", message = "必须为4到32位字母、数字或下划线") String username,
            @NotBlank @Size(min = 8, max = 64) @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "必须同时包含字母和数字") String password,
            @NotBlank @Size(max = 40) String displayName
    ) {
    }

    public record LoginResponse(String token, LocalDateTime expiresAt, MeView user) {
    }

    public record PasswordChangeRequest(
            @NotBlank @Size(max = 64) String oldPassword,
            @NotBlank @Size(min = 8, max = 64) @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "必须同时包含字母和数字") String newPassword
    ) {
    }

    public record MeView(
            Long id, Long departmentId, String username, String displayName, String roleCode,
            String dataScope, Set<String> permissions
    ) {
    }

    public record ChildUpsertRequest(
            @NotBlank @Size(max = 30) String name,
            @NotBlank @Pattern(regexp = "MALE|FEMALE|OTHER", message = "不正确") String gender,
            @NotNull @Past LocalDate birthDate,
            @NotBlank @Size(max = 100) String region,
            @NotBlank @Size(max = 30) String schoolStage,
            @NotBlank @Size(max = 40) String guardianName,
            @NotBlank @Pattern(regexp = "^1\\d{10}$", message = "格式不正确") String guardianPhone,
            @NotBlank @Size(max = 200) String address,
            @NotBlank @Size(max = 1000) String familySummary,
            @NotBlank @Pattern(regexp = "LOW|MEDIUM|HIGH", message = "不正确") String riskLevel,
            Integer version
    ) {
    }

    public record ReviewRequest(
            @NotBlank @Pattern(regexp = "APPROVED|REJECTED", message = "不正确") String decision,
            @Size(max = 500) String comment,
            @NotNull Integer version
    ) {
    }

    public record DecisionRequest(
            @NotBlank @Pattern(regexp = "APPROVED|REJECTED", message = "不正确") String decision,
            @Size(max = 500) String comment
    ) {
    }

    public record VersionRequest(@NotNull Integer version) {
    }

    public record CancelRequest(
            @NotNull Integer version,
            @NotBlank @Size(max = 500) String reason
    ) {
    }

    public record ChildView(
            Long id, String fileNo, Long departmentId, String departmentName, String name, String gender,
            LocalDate birthDate, String region, String schoolStage, String guardianName, String guardianPhone,
            String address, String familySummary, String riskLevel, String status, String rejectionReason,
            Long createdBy, String creatorName, int version, LocalDateTime createdAt, LocalDateTime updatedAt
    ) {
    }

    public record VolunteerProfileRequest(
            @NotBlank @Size(max = 40) String realName,
            @NotBlank @Pattern(regexp = "^1\\d{10}$", message = "格式不正确") String phone,
            @NotBlank @Size(max = 100) String serviceRegion,
            @NotBlank @Size(max = 500) String skills,
            @NotBlank @Size(max = 300) String availableTime,
            @NotBlank @Size(max = 1000) String introduction
    ) {
    }

    public record VolunteerView(
            Long userId, String username, String displayName, String realName, String phone,
            String serviceRegion, String skills, String availableTime, String introduction,
            String certificationStatus, String rejectionReason, LocalDateTime updatedAt
    ) {
    }

    public record AidUpsertRequest(
            @NotNull Long childId,
            @NotBlank @Pattern(regexp = "EDUCATION|COMPANIONSHIP|LIFE_CARE|SAFETY|PSYCHOLOGICAL|OTHER", message = "不正确") String category,
            @NotBlank @Size(max = 120) String title,
            @NotBlank @Size(max = 2000) String description,
            @NotBlank @Size(max = 300) String publicSummary,
            @NotBlank @Pattern(regexp = "NORMAL|URGENT", message = "不正确") String priority,
            Integer version
    ) {
    }

    public record AidView(
            Long id, String requestNo, Long childId, String childFileNo, String childName,
            Long departmentId, String departmentName, String category, String title, String description,
            String publicSummary, String priority, String status, String rejectionReason, Long createdBy, String creatorName,
            int version, LocalDateTime createdAt, LocalDateTime updatedAt
    ) {
    }

    public record PublicAidView(
            Long id, String requestNo, String ageGroup, String region, String category,
            String title, String summary, String priority, LocalDateTime createdAt
    ) {
    }

    public record ApplicationCreateRequest(@NotBlank @Size(max = 500) String message) {
    }

    public record AcceptApplicationRequest(@NotNull Integer requestVersion) {
    }

    public record ApplicationView(
            Long id, Long requestId, String requestNo, String requestTitle, Long volunteerId,
            String volunteerName, String message, String status, LocalDateTime decidedAt, LocalDateTime createdAt
    ) {
    }

    public record VisitCreateRequest(
            @NotNull LocalDate serviceDate,
            @Min(1) @Max(1440) int durationMinutes,
            @NotBlank @Size(max = 1500) String content,
            @NotBlank @Size(max = 1000) String result
    ) {
    }

    public record CompletionRequest(@NotBlank @Size(max = 1500) String summary, @NotNull Integer version) {
    }

    public record FeedbackRequest(
            @Min(1) @Max(5) int rating,
            @NotBlank @Size(max = 1000) String comment,
            @NotNull Integer requestVersion
    ) {
    }

    public record AssignmentView(
            Long id, Long requestId, String requestNo, String requestTitle, String category,
            Long volunteerId, String volunteerName, Long departmentId, String childFileNo,
            String childName, String guardianName, String guardianPhone, String address, String region,
            String status, LocalDateTime startedAt, String completionSummary,
            LocalDateTime submittedAt, LocalDateTime completedAt, int version, int requestVersion,
            LocalDateTime createdAt, List<VisitView> visits
    ) {
    }

    public record VisitView(
            Long id, LocalDate serviceDate, int durationMinutes, String content,
            String result, String creatorName, LocalDateTime createdAt
    ) {
    }

    public record UserStatusRequest(
            @NotBlank @Pattern(regexp = "ACTIVE|DISABLED", message = "不正确") String status
    ) {
    }

    public record UserAdminView(
            Long id, String username, String displayName, String roleCode, String roleName,
            Long departmentId, String departmentName, String status,
            LocalDateTime lastLoginAt, LocalDateTime createdAt
    ) {
    }

    public record AdminCreateUserRequest(
            @NotBlank @Pattern(regexp = "[A-Za-z0-9_]{4,32}", message = "必须为4到32位字母、数字或下划线") String username,
            @NotBlank @Size(min = 8, max = 64) @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "必须同时包含字母和数字") String password,
            @NotBlank @Size(max = 40) String displayName,
            @NotNull Long roleId,
            Long departmentId
    ) {
    }

    public record DepartmentRequest(
            @NotBlank @Pattern(regexp = "[A-Z0-9_]{2,32}", message = "必须为2到32位大写字母、数字或下划线") String code,
            @NotBlank @Size(max = 100) String name
    ) {
    }

    public record RolePermissionRequest(@NotNull Set<Long> permissionIds) {
    }

    public record RoleView(Long id, String code, String name, String dataScope, List<Long> permissionIds) {
    }

    public record PublicOverview(
            long completedServices, long approvedVolunteers, long activeRequests, long serviceDepartments
    ) {
    }

    public record DashboardView(
            long activeChildren, long pendingRequests, long openRequests, long activeVolunteers,
            long activeAssignments, long completedAssignments
    ) {
    }
}
