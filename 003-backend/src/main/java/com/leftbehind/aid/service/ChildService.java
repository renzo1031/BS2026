package com.leftbehind.aid.service;

import com.leftbehind.aid.api.ApiModels;
import com.leftbehind.aid.common.BusinessException;
import com.leftbehind.aid.common.PageResult;
import com.leftbehind.aid.domain.Domain;
import com.leftbehind.aid.mapper.ChildMapper;
import com.leftbehind.aid.security.PlatformPrincipal;
import com.leftbehind.aid.security.SecurityUtils;
import com.leftbehind.aid.security.SensitiveDataService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class ChildService {
    private final ChildMapper childMapper;
    private final SensitiveDataService sensitiveData;
    private final AccessService accessService;
    private final AuditService auditService;

    public ChildService(ChildMapper childMapper, SensitiveDataService sensitiveData,
                        AccessService accessService, AuditService auditService) {
        this.childMapper = childMapper;
        this.sensitiveData = sensitiveData;
        this.accessService = accessService;
        this.auditService = auditService;
    }

    @Transactional
    public ApiModels.ChildView create(ApiModels.ChildUpsertRequest request) {
        validateAge(request.birthDate());
        PlatformPrincipal principal = SecurityUtils.current();
        Long departmentId = accessService.requireCurrentDepartment();
        String fileNo = "CH" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        childMapper.insert(fileNo, departmentId, sensitiveData.encrypt(request.name().trim()), request.gender(),
                request.birthDate(), request.region().trim(), request.schoolStage().trim(),
                sensitiveData.encrypt(request.guardianName().trim()), sensitiveData.encrypt(request.guardianPhone()),
                sensitiveData.encrypt(request.address().trim()), request.familySummary().trim(), request.riskLevel(),
                principal.id());
        Domain.ChildRow child = childMapper.findByFileNo(fileNo);
        auditService.log("CREATE_CHILD", "CHILD", child.id(), null, child.status(), "创建儿童档案草稿");
        return toView(child);
    }

    public PageResult<ApiModels.ChildView> list(int page, int size, String status, String keyword) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Long departmentId = accessService.queryDepartment();
        Long ownerId = accessService.queryOwnerForCaseWorker();
        List<ApiModels.ChildView> items = childMapper.list(departmentId, ownerId, clean(status), clean(keyword),
                        (safePage - 1) * safeSize, safeSize).stream().map(this::toView).toList();
        return new PageResult<>(items, childMapper.count(departmentId, ownerId, clean(status), clean(keyword)),
                safePage, safeSize);
    }

    public ApiModels.ChildView detail(Long id) {
        return toView(requireReadable(id));
    }

    @Transactional
    public ApiModels.ChildView update(Long id, ApiModels.ChildUpsertRequest request) {
        if (request.version() == null) {
            throw BusinessException.badRequest("version不能为空");
        }
        validateAge(request.birthDate());
        Domain.ChildRow child = requireReadable(id);
        accessService.requireOwner(child.createdBy());
        BusinessRules.requireState(child.status(), "DRAFT", "REJECTED");
        int affected = childMapper.update(id, SecurityUtils.current().id(), request.version(),
                sensitiveData.encrypt(request.name().trim()), request.gender(), request.birthDate(),
                request.region().trim(), request.schoolStage().trim(), sensitiveData.encrypt(request.guardianName().trim()),
                sensitiveData.encrypt(request.guardianPhone()), sensitiveData.encrypt(request.address().trim()),
                request.familySummary().trim(), request.riskLevel());
        BusinessRules.requireUpdated(affected);
        auditService.log("UPDATE_CHILD", "CHILD", id, child.status(), "DRAFT", "更新儿童档案草稿");
        return toView(childMapper.findById(id));
    }

    @Transactional
    public void submit(Long id, int version) {
        Domain.ChildRow child = requireReadable(id);
        accessService.requireOwner(child.createdBy());
        BusinessRules.requireState(child.status(), "DRAFT", "REJECTED");
        BusinessRules.requireUpdated(childMapper.submit(id, SecurityUtils.current().id(), version));
        auditService.log("SUBMIT_CHILD", "CHILD", id, child.status(), "PENDING_REVIEW", "提交儿童档案审核");
    }

    @Transactional
    public void review(Long id, ApiModels.ReviewRequest request) {
        Domain.ChildRow child = requireReadable(id);
        BusinessRules.requireState(child.status(), "PENDING_REVIEW");
        if (child.createdBy().equals(SecurityUtils.current().id())) {
            throw BusinessException.forbidden("不能审核本人提交的档案");
        }
        String next = "APPROVED".equals(request.decision()) ? "ACTIVE" : "REJECTED";
        String rejectionReason = BusinessRules.rejectionReason(request.decision(), request.comment());
        BusinessRules.requireUpdated(childMapper.review(id, accessService.requireCurrentDepartment(),
                SecurityUtils.current().id(), next, rejectionReason, request.version()));
        auditService.log("REVIEW_CHILD", "CHILD", id, child.status(), next, "儿童档案审核");
    }

    @Transactional
    public void archive(Long id, int version) {
        Domain.ChildRow child = requireReadable(id);
        accessService.requireOwner(child.createdBy());
        BusinessRules.requireState(child.status(), "ACTIVE");
        if (childMapper.countOpenAidRequests(id) > 0) {
            throw BusinessException.conflict("仍有未结束的帮扶需求，不能归档");
        }
        BusinessRules.requireUpdated(childMapper.archive(id, SecurityUtils.current().id(), version));
        auditService.log("ARCHIVE_CHILD", "CHILD", id, child.status(), "ARCHIVED", "归档儿童档案");
    }

    public Domain.ChildRow requireReadable(Long id) {
        Domain.ChildRow child = childMapper.findById(id);
        if (child == null) {
            throw BusinessException.notFound("儿童档案不存在");
        }
        accessService.requireDepartment(child.departmentId());
        if (SecurityUtils.current().hasRole("CASE_WORKER")) {
            accessService.requireOwner(child.createdBy());
        }
        return child;
    }

    private ApiModels.ChildView toView(Domain.ChildRow child) {
        return new ApiModels.ChildView(child.id(), child.fileNo(), child.departmentId(), child.departmentName(),
                sensitiveData.decrypt(child.nameEncrypted()), child.gender(), child.birthDate(), child.region(),
                child.schoolStage(), sensitiveData.decrypt(child.guardianNameEncrypted()),
                sensitiveData.decrypt(child.guardianPhoneEncrypted()), sensitiveData.decrypt(child.addressEncrypted()),
                child.familySummary(), child.riskLevel(), child.status(), child.rejectionReason(), child.createdBy(),
                child.creatorName(), child.version(), child.createdAt(), child.updatedAt());
    }

    private void validateAge(LocalDate birthDate) {
        if (!birthDate.isAfter(LocalDate.now().minusYears(18))) {
            throw BusinessException.badRequest("档案对象年龄必须小于18周岁");
        }
    }

    private static String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
