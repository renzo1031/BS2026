package com.leftbehind.aid.service;

import com.leftbehind.aid.api.ApiModels;
import com.leftbehind.aid.common.BusinessException;
import com.leftbehind.aid.common.PageResult;
import com.leftbehind.aid.domain.Domain;
import com.leftbehind.aid.mapper.AidMapper;
import com.leftbehind.aid.mapper.SystemMapper;
import com.leftbehind.aid.security.PlatformPrincipal;
import com.leftbehind.aid.security.SecurityUtils;
import com.leftbehind.aid.security.SensitiveDataService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class AidService {
    private final AidMapper aidMapper;
    private final SystemMapper systemMapper;
    private final ChildService childService;
    private final AccessService accessService;
    private final SensitiveDataService sensitiveData;
    private final AuditService auditService;

    public AidService(AidMapper aidMapper, SystemMapper systemMapper, ChildService childService,
                      AccessService accessService, SensitiveDataService sensitiveData, AuditService auditService) {
        this.aidMapper = aidMapper;
        this.systemMapper = systemMapper;
        this.childService = childService;
        this.accessService = accessService;
        this.sensitiveData = sensitiveData;
        this.auditService = auditService;
    }

    @Transactional
    public ApiModels.AidView create(ApiModels.AidUpsertRequest request) {
        BusinessRules.requirePublicSummarySafe(request.publicSummary());
        Domain.ChildRow child = childService.requireReadable(request.childId());
        accessService.requireOwner(child.createdBy());
        BusinessRules.requireState(child.status(), "ACTIVE");
        String requestNo = "AR" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        aidMapper.insertAid(requestNo, child.id(), child.departmentId(), request.category(), request.title().trim(),
                request.description().trim(), request.publicSummary().trim(), request.priority(), SecurityUtils.current().id());
        Domain.AidRow aid = aidMapper.findAidByRequestNo(requestNo);
        auditService.log("CREATE_AID", "AID_REQUEST", aid.id(), null, aid.status(), "创建帮扶需求草稿");
        return toView(aid);
    }

    public PageResult<ApiModels.AidView> list(int page, int size, String status, String category, String keyword) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Long departmentId = accessService.queryDepartment();
        Long ownerId = accessService.queryOwnerForCaseWorker();
        List<ApiModels.AidView> items = aidMapper.listAid(departmentId, ownerId, clean(status), clean(category),
                        clean(keyword), (safePage - 1) * safeSize, safeSize).stream().map(this::toView).toList();
        return new PageResult<>(items,
                aidMapper.countAid(departmentId, ownerId, clean(status), clean(category), clean(keyword)),
                safePage, safeSize);
    }

    public ApiModels.AidView detail(Long id) {
        return toView(requireReadable(id));
    }

    public PageResult<ApiModels.PublicAidView> publicList(int page, int size, String category, String keyword) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 50);
        List<ApiModels.PublicAidView> items = aidMapper.listPublicAid(clean(category), clean(keyword),
                        (safePage - 1) * safeSize, safeSize)
                .stream().map(this::toPublicView).toList();
        return new PageResult<>(items, aidMapper.countPublicAid(clean(category), clean(keyword)), safePage, safeSize);
    }

    public ApiModels.PublicAidView publicDetail(Long id) {
        Domain.AidRow aid = aidMapper.findAidById(id);
        if (aid == null || !"APPROVED".equals(aid.status())) {
            throw BusinessException.notFound("公开需求不存在或已结束");
        }
        return toPublicView(aid);
    }

    @Transactional
    public ApiModels.AidView update(Long id, ApiModels.AidUpsertRequest request) {
        if (request.version() == null) {
            throw BusinessException.badRequest("version不能为空");
        }
        BusinessRules.requirePublicSummarySafe(request.publicSummary());
        Domain.AidRow aid = requireReadable(id);
        accessService.requireOwner(aid.createdBy());
        BusinessRules.requireState(aid.status(), "DRAFT", "REJECTED");
        Domain.ChildRow child = childService.requireReadable(request.childId());
        accessService.requireOwner(child.createdBy());
        BusinessRules.requireState(child.status(), "ACTIVE");
        BusinessRules.requireUpdated(aidMapper.updateAid(id, SecurityUtils.current().id(), request.version(),
                child.id(), child.departmentId(), request.category(), request.title().trim(),
                request.description().trim(), request.publicSummary().trim(), request.priority()));
        auditService.log("UPDATE_AID", "AID_REQUEST", id, aid.status(), "DRAFT", "更新帮扶需求草稿");
        return toView(aidMapper.findAidById(id));
    }

    @Transactional
    public void submit(Long id, int version) {
        Domain.AidRow aid = requireReadable(id);
        accessService.requireOwner(aid.createdBy());
        BusinessRules.requireState(aid.status(), "DRAFT", "REJECTED");
        BusinessRules.requireUpdated(aidMapper.submitAid(id, SecurityUtils.current().id(), version));
        auditService.log("SUBMIT_AID", "AID_REQUEST", id, aid.status(), "PENDING_REVIEW", "提交帮扶需求审核");
    }

    @Transactional
    public void review(Long id, ApiModels.ReviewRequest request) {
        Domain.AidRow aid = requireReadable(id);
        BusinessRules.requireState(aid.status(), "PENDING_REVIEW");
        if (aid.createdBy().equals(SecurityUtils.current().id())) {
            throw BusinessException.forbidden("不能审核本人提交的需求");
        }
        String reviewComment = clean(request.comment());
        String rejectionReason = BusinessRules.rejectionReason(request.decision(), request.comment());
        BusinessRules.requireUpdated(aidMapper.reviewAid(id, accessService.requireCurrentDepartment(),
                SecurityUtils.current().id(), request.decision(), rejectionReason, request.version()));
        aidMapper.insertReview(id, SecurityUtils.current().id(), request.decision(), reviewComment);
        auditService.log("REVIEW_AID", "AID_REQUEST", id, aid.status(), request.decision(), "帮扶需求审核");
    }

    @Transactional
    public void cancel(Long id, ApiModels.CancelRequest request) {
        Domain.AidRow aid = requireReadable(id);
        BusinessRules.requireState(aid.status(), "DRAFT", "PENDING_REVIEW", "APPROVED", "MATCHED");
        PlatformPrincipal principal = SecurityUtils.current();
        if (principal.hasRole("CASE_WORKER")) {
            accessService.requireOwner(aid.createdBy());
        } else if (!principal.hasRole("SUPERVISOR")) {
            throw BusinessException.forbidden("当前角色不能取消帮扶需求");
        }
        if ("MATCHED".equals(aid.status())) {
            Domain.AssignmentRow assignment = aidMapper.findActiveAssignmentByRequest(id);
            if (assignment == null) {
                throw BusinessException.conflict("有效服务任务不存在，请刷新后重试");
            }
            BusinessRules.requireState(assignment.status(), "ASSIGNED");
            BusinessRules.requireUpdated(aidMapper.terminateUnstartedAssignment(
                    assignment.id(), assignment.version(), request.reason().trim()));
        }
        BusinessRules.requireUpdated(aidMapper.cancelAid(id, aid.status(), request.version()));
        auditService.log("CANCEL_AID", "AID_REQUEST", id, aid.status(), "CANCELLED", "取消帮扶需求");
    }

    @Transactional
    public void apply(Long requestId, ApiModels.ApplicationCreateRequest request) {
        PlatformPrincipal principal = SecurityUtils.current();
        Domain.VolunteerRow volunteer = systemMapper.findVolunteer(principal.id());
        if (volunteer == null || !"APPROVED".equals(volunteer.certificationStatus())) {
            throw BusinessException.forbidden("志愿者认证通过后才能申请帮扶需求");
        }
        Domain.AidRow aid = aidMapper.findAidById(requestId);
        if (aid == null) {
            throw BusinessException.notFound("帮扶需求不存在");
        }
        BusinessRules.requireState(aid.status(), "APPROVED");
        try {
            aidMapper.insertApplication(requestId, principal.id(), request.message().trim());
        } catch (DuplicateKeyException exception) {
            throw BusinessException.conflict("你已经申请过该需求");
        }
        auditService.log("APPLY_AID", "AID_APPLICATION", requestId, null, "APPLIED", "志愿者提交申请");
    }

    public List<ApiModels.ApplicationView> myApplications() {
        return aidMapper.listApplicationsForVolunteer(SecurityUtils.current().id()).stream()
                .map(this::toApplicationView).toList();
    }

    public List<ApiModels.ApplicationView> applicationsForRequest(Long requestId) {
        Domain.AidRow aid = requireReadable(requestId);
        accessService.requireDepartment(aid.departmentId());
        return aidMapper.listApplicationsForRequest(requestId).stream().map(this::toApplicationView).toList();
    }

    @Transactional
    public void withdrawApplication(Long applicationId) {
        Domain.ApplicationRow application = aidMapper.findApplication(applicationId);
        if (application == null) {
            throw BusinessException.notFound("申请记录不存在");
        }
        BusinessRules.requireState(application.status(), "APPLIED");
        BusinessRules.requireUpdated(aidMapper.withdrawApplication(applicationId, SecurityUtils.current().id()));
        auditService.log("WITHDRAW_APPLICATION", "AID_APPLICATION", applicationId,
                application.status(), "WITHDRAWN", "志愿者撤回申请");
    }

    @Transactional
    public Long acceptApplication(Long applicationId, int requestVersion) {
        Domain.ApplicationRow application = aidMapper.findApplication(applicationId);
        if (application == null) {
            throw BusinessException.notFound("申请记录不存在");
        }
        Domain.AidRow aid = aidMapper.findAidByIdForUpdate(application.requestId());
        if (aid == null) {
            throw BusinessException.notFound("帮扶需求不存在");
        }
        accessService.requireDepartment(aid.departmentId());
        BusinessRules.requireState(aid.status(), "APPROVED");
        BusinessRules.requireState(application.status(), "APPLIED");
        if (aid.version() != requestVersion) {
            throw BusinessException.conflict("需求状态已变化，请刷新后重试");
        }
        BusinessRules.requireUpdated(aidMapper.acceptApplication(applicationId, aid.id(), SecurityUtils.current().id()));
        aidMapper.rejectOtherApplications(aid.id(), applicationId, SecurityUtils.current().id());
        aidMapper.insertAssignment(aid.id(), applicationId, application.volunteerId(), aid.departmentId());
        BusinessRules.requireUpdated(aidMapper.updateAidStatus(aid.id(), "APPROVED", "MATCHED", aid.version()));
        Domain.AssignmentRow assignment = aidMapper.findAssignmentByApplication(applicationId);
        auditService.log("MATCH_VOLUNTEER", "AID_REQUEST", aid.id(), aid.status(), "MATCHED", "接受志愿申请并创建任务");
        return assignment.id();
    }

    public Domain.AidRow requireReadable(Long id) {
        Domain.AidRow aid = aidMapper.findAidById(id);
        if (aid == null) {
            throw BusinessException.notFound("帮扶需求不存在");
        }
        accessService.requireDepartment(aid.departmentId());
        if (SecurityUtils.current().hasRole("CASE_WORKER")) {
            accessService.requireOwner(aid.createdBy());
        }
        return aid;
    }

    private ApiModels.AidView toView(Domain.AidRow aid) {
        return new ApiModels.AidView(aid.id(), aid.requestNo(), aid.childId(), aid.childFileNo(),
                sensitiveData.decrypt(aid.childNameEncrypted()), aid.departmentId(), aid.departmentName(),
                aid.category(), aid.title(), aid.description(), aid.publicSummary(), aid.priority(), aid.status(),
                aid.rejectionReason(), aid.createdBy(), aid.creatorName(), aid.version(), aid.createdAt(), aid.updatedAt());
    }

    private ApiModels.PublicAidView toPublicView(Domain.AidRow aid) {
        int age = Period.between(aid.birthDate(), LocalDate.now()).getYears();
        String ageGroup = age < 6 ? "6岁以下" : age <= 9 ? "6至9岁" : age <= 12 ? "10至12岁" : "13至17岁";
        return new ApiModels.PublicAidView(aid.id(), aid.requestNo(), ageGroup, aid.region(), aid.category(),
                aid.title(), aid.publicSummary(), aid.priority(), aid.createdAt());
    }

    private ApiModels.ApplicationView toApplicationView(Domain.ApplicationRow row) {
        return new ApiModels.ApplicationView(row.id(), row.requestId(), row.requestNo(), row.requestTitle(),
                row.volunteerId(), row.volunteerName(), row.message(), row.status(), row.decidedAt(), row.createdAt());
    }

    private static String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
