package com.leftbehind.aid.service;

import com.leftbehind.aid.api.ApiModels;
import com.leftbehind.aid.common.BusinessException;
import com.leftbehind.aid.common.PageResult;
import com.leftbehind.aid.domain.Domain;
import com.leftbehind.aid.mapper.AidMapper;
import com.leftbehind.aid.security.PlatformPrincipal;
import com.leftbehind.aid.security.SecurityUtils;
import com.leftbehind.aid.security.SensitiveDataService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Service
public class AssignmentService {
    private final AidMapper aidMapper;
    private final AccessService accessService;
    private final SensitiveDataService sensitiveData;
    private final AuditService auditService;

    public AssignmentService(AidMapper aidMapper, AccessService accessService,
                             SensitiveDataService sensitiveData, AuditService auditService) {
        this.aidMapper = aidMapper;
        this.accessService = accessService;
        this.sensitiveData = sensitiveData;
        this.auditService = auditService;
    }

    public PageResult<ApiModels.AssignmentView> list(int page, int size, String status) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        PlatformPrincipal principal = SecurityUtils.current();
        Long volunteerId = principal.hasRole("VOLUNTEER") ? principal.id() : null;
        Long departmentId = principal.hasRole("SUPERVISOR") || principal.hasRole("CASE_WORKER")
                ? principal.departmentId() : null;
        Long ownerId = principal.hasRole("CASE_WORKER") ? principal.id() : null;
        List<ApiModels.AssignmentView> items = aidMapper.listAssignments(volunteerId, departmentId, ownerId,
                        clean(status), (safePage - 1) * safeSize, safeSize)
                .stream().map(row -> toView(row, List.of())).toList();
        return new PageResult<>(items,
                aidMapper.countAssignments(volunteerId, departmentId, ownerId, clean(status)), safePage, safeSize);
    }

    public ApiModels.AssignmentView detail(Long id) {
        Domain.AssignmentRow assignment = requireReadable(id);
        return toView(assignment, aidMapper.listVisits(id));
    }

    @Transactional
    public void start(Long id, int version) {
        Domain.AssignmentRow assignment = requireVolunteerOwner(id);
        BusinessRules.requireState(assignment.status(), "ASSIGNED");
        Domain.AidRow aid = aidMapper.findAidById(assignment.requestId());
        BusinessRules.requireState(aid.status(), "MATCHED");
        BusinessRules.requireUpdated(aidMapper.startAssignment(id, SecurityUtils.current().id(), version));
        BusinessRules.requireUpdated(aidMapper.updateAidStatus(aid.id(), "MATCHED", "IN_PROGRESS", aid.version()));
        auditService.log("START_ASSIGNMENT", "ASSIGNMENT", id, assignment.status(), "IN_PROGRESS", "志愿者开始服务");
    }

    @Transactional
    public void addVisit(Long id, ApiModels.VisitCreateRequest request) {
        Domain.AssignmentRow assignment = requireVolunteerOwner(id);
        BusinessRules.requireState(assignment.status(), "IN_PROGRESS");
        if (request.serviceDate().isAfter(LocalDate.now())) {
            throw BusinessException.badRequest("服务日期不能晚于今天");
        }
        if (assignment.startedAt() != null && request.serviceDate().isBefore(assignment.startedAt().toLocalDate())) {
            throw BusinessException.badRequest("服务日期不能早于任务开始日期");
        }
        aidMapper.insertVisit(id, request.serviceDate(), request.durationMinutes(), request.content().trim(),
                request.result().trim(), SecurityUtils.current().id());
        auditService.log("ADD_VISIT", "ASSIGNMENT", id, assignment.status(), assignment.status(), "新增服务回访记录");
    }

    @Transactional
    public void submitCompletion(Long id, ApiModels.CompletionRequest request) {
        Domain.AssignmentRow assignment = requireVolunteerOwner(id);
        BusinessRules.requireState(assignment.status(), "IN_PROGRESS");
        if (aidMapper.countVisits(id) < 1) {
            throw BusinessException.conflict("至少提交一条回访记录后才能申请完成");
        }
        Domain.AidRow aid = aidMapper.findAidById(assignment.requestId());
        BusinessRules.requireState(aid.status(), "IN_PROGRESS");
        BusinessRules.requireUpdated(aidMapper.submitCompletion(id, SecurityUtils.current().id(),
                request.summary().trim(), request.version()));
        BusinessRules.requireUpdated(aidMapper.updateAidStatus(aid.id(), "IN_PROGRESS", "PENDING_ACCEPTANCE", aid.version()));
        auditService.log("SUBMIT_COMPLETION", "ASSIGNMENT", id, assignment.status(),
                "PENDING_ACCEPTANCE", "志愿者提交完成验收");
    }

    @Transactional
    public void confirm(Long id, int version) {
        Domain.AssignmentRow assignment = requireReadable(id);
        accessService.requireDepartment(assignment.departmentId());
        BusinessRules.requireState(assignment.status(), "PENDING_ACCEPTANCE");
        Domain.AidRow aid = aidMapper.findAidById(assignment.requestId());
        BusinessRules.requireState(aid.status(), "PENDING_ACCEPTANCE");
        BusinessRules.requireUpdated(aidMapper.confirmCompletion(id, accessService.requireCurrentDepartment(), version));
        BusinessRules.requireUpdated(aidMapper.updateAidStatus(aid.id(), "PENDING_ACCEPTANCE", "COMPLETED", aid.version()));
        auditService.log("CONFIRM_COMPLETION", "ASSIGNMENT", id, assignment.status(), "COMPLETED", "主管验收服务完成");
    }

    @Transactional
    public void feedback(Long id, ApiModels.FeedbackRequest request) {
        Domain.AssignmentRow assignment = requireReadable(id);
        Domain.AidRow aid = aidMapper.findAidById(assignment.requestId());
        accessService.requireOwner(aid.createdBy());
        BusinessRules.requireState(assignment.status(), "COMPLETED");
        BusinessRules.requireState(aid.status(), "COMPLETED");
        aidMapper.insertFeedback(id, request.rating(), request.comment().trim(), SecurityUtils.current().id());
        BusinessRules.requireUpdated(aidMapper.updateAidStatus(aid.id(), "COMPLETED", "CLOSED", request.requestVersion()));
        auditService.log("ADD_FEEDBACK", "ASSIGNMENT", id, "COMPLETED", "CLOSED", "个案人员完成服务评价");
    }

    private Domain.AssignmentRow requireVolunteerOwner(Long id) {
        Domain.AssignmentRow assignment = aidMapper.findAssignment(id);
        if (assignment == null) {
            throw BusinessException.notFound("服务任务不存在");
        }
        if (!Objects.equals(assignment.volunteerId(), SecurityUtils.current().id())) {
            throw BusinessException.forbidden("只能操作分配给本人的服务任务");
        }
        return assignment;
    }

    private Domain.AssignmentRow requireReadable(Long id) {
        Domain.AssignmentRow assignment = aidMapper.findAssignment(id);
        if (assignment == null) {
            throw BusinessException.notFound("服务任务不存在");
        }
        PlatformPrincipal principal = SecurityUtils.current();
        if (principal.hasRole("VOLUNTEER")) {
            if (!Objects.equals(assignment.volunteerId(), principal.id())) {
                throw BusinessException.forbidden("无权查看他人的服务任务");
            }
        } else {
            accessService.requireDepartment(assignment.departmentId());
            if (principal.hasRole("CASE_WORKER")) {
                Domain.AidRow aid = aidMapper.findAidById(assignment.requestId());
                accessService.requireOwner(aid.createdBy());
            }
        }
        return assignment;
    }

    private ApiModels.AssignmentView toView(Domain.AssignmentRow row, List<Domain.VisitRow> visits) {
        List<ApiModels.VisitView> visitViews = visits.stream().map(visit -> new ApiModels.VisitView(
                visit.id(), visit.serviceDate(), visit.durationMinutes(), visit.content(), visit.result(),
                visit.creatorName(), visit.createdAt())).toList();
        return new ApiModels.AssignmentView(row.id(), row.requestId(), row.requestNo(), row.requestTitle(),
                row.category(), row.volunteerId(), row.volunteerName(), row.departmentId(), row.childFileNo(),
                sensitiveData.decrypt(row.childNameEncrypted()), sensitiveData.decrypt(row.guardianNameEncrypted()),
                sensitiveData.decrypt(row.guardianPhoneEncrypted()), sensitiveData.decrypt(row.addressEncrypted()),
                row.region(), row.status(), row.startedAt(), row.completionSummary(), row.submittedAt(),
                row.completedAt(), row.version(), row.requestVersion(), row.createdAt(), visitViews);
    }

    private static String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
