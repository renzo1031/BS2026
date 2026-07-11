package com.leftbehind.aid.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

public final class Domain {
    private Domain() {
    }

    public enum RoleCode { SYS_ADMIN, SUPERVISOR, CASE_WORKER, VOLUNTEER }

    public enum ChildStatus { DRAFT, PENDING_REVIEW, ACTIVE, REJECTED, ARCHIVED }

    public enum VolunteerStatus { UNVERIFIED, PENDING_REVIEW, APPROVED, REJECTED, SUSPENDED }

    public enum AidStatus {
        DRAFT, PENDING_REVIEW, APPROVED, REJECTED, MATCHED, IN_PROGRESS,
        PENDING_ACCEPTANCE, COMPLETED, CLOSED, CANCELLED
    }

    public enum ApplicationStatus { APPLIED, ACCEPTED, REJECTED, WITHDRAWN }

    public enum AssignmentStatus { ASSIGNED, IN_PROGRESS, PENDING_ACCEPTANCE, COMPLETED, TERMINATED }

    public record UserRow(
            Long id, Long departmentId, Long roleId, String username, String passwordHash, String displayName,
            String status, int failedLoginCount, LocalDateTime lockedUntil, String roleCode, String dataScope,
            LocalDateTime createdAt
    ) {
    }

    public record UserAdminRow(
            Long id, String username, String displayName, String roleCode, String roleName,
            Long departmentId, String departmentName, String status,
            LocalDateTime lastLoginAt, LocalDateTime createdAt
    ) {
    }

    public record DepartmentRow(Long id, String code, String name, boolean enabled, LocalDateTime createdAt) {
    }

    public record RoleRow(Long id, String code, String name, String dataScope) {
    }

    public record PermissionRow(Long id, String code, String name, String module) {
    }

    public record ChildRow(
            Long id, String fileNo, Long departmentId, String departmentName, String nameEncrypted,
            String gender, LocalDate birthDate, String region, String schoolStage,
            String guardianNameEncrypted, String guardianPhoneEncrypted, String addressEncrypted,
            String familySummary, String riskLevel, String status, String rejectionReason,
            Long createdBy, String creatorName, Long reviewedBy, LocalDateTime reviewedAt,
            int version, LocalDateTime createdAt, LocalDateTime updatedAt
    ) {
    }

    public record VolunteerRow(
            Long userId, String username, String displayName, String realNameEncrypted, String phoneEncrypted,
            String serviceRegion, String skills, String availableTime, String introduction,
            String certificationStatus, String rejectionReason, Long reviewedBy, LocalDateTime reviewedAt,
            LocalDateTime createdAt, LocalDateTime updatedAt
    ) {
    }

    public record AidRow(
            Long id, String requestNo, Long childId, String childFileNo, String childNameEncrypted,
            LocalDate birthDate, String region, Long departmentId, String departmentName,
            String category, String title, String description, String publicSummary, String priority, String status,
            String rejectionReason, Long createdBy, String creatorName, Long reviewedBy,
            LocalDateTime reviewedAt, int version, LocalDateTime createdAt, LocalDateTime updatedAt
    ) {
    }

    public record ApplicationRow(
            Long id, Long requestId, String requestNo, String requestTitle, Long volunteerId,
            String volunteerName, String message, String status, Long decidedBy,
            LocalDateTime decidedAt, LocalDateTime createdAt
    ) {
    }

    public record AssignmentRow(
            Long id, Long requestId, String requestNo, String requestTitle, String category,
            Long volunteerId, String volunteerName, Long departmentId, Long childId, String childFileNo,
            String childNameEncrypted, String guardianNameEncrypted, String guardianPhoneEncrypted,
            String addressEncrypted, String region, String status, LocalDateTime startedAt,
            String completionSummary, LocalDateTime submittedAt, LocalDateTime completedAt,
            int version, int requestVersion, LocalDateTime createdAt, LocalDateTime updatedAt
    ) {
    }

    public record VisitRow(
            Long id, Long assignmentId, LocalDate serviceDate, int durationMinutes, String content,
            String result, Long createdBy, String creatorName, LocalDateTime createdAt
    ) {
    }

    public record AuditRow(
            Long id, Long userId, String username, String action, String businessType, String businessId,
            String beforeStatus, String afterStatus, String detail, String ipAddress, LocalDateTime createdAt
    ) {
    }

    public record DashboardStats(
            long activeChildren, long pendingRequests, long openRequests, long activeVolunteers,
            long activeAssignments, long completedAssignments
    ) {
    }

    public record PublicStats(
            long completedServices, long approvedVolunteers, long activeRequests, long serviceDepartments
    ) {
    }
}
