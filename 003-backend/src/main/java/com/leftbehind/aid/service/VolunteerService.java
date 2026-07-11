package com.leftbehind.aid.service;

import com.leftbehind.aid.api.ApiModels;
import com.leftbehind.aid.common.BusinessException;
import com.leftbehind.aid.domain.Domain;
import com.leftbehind.aid.mapper.SystemMapper;
import com.leftbehind.aid.security.SecurityUtils;
import com.leftbehind.aid.security.SensitiveDataService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VolunteerService {
    private final SystemMapper systemMapper;
    private final SensitiveDataService sensitiveData;
    private final AuditService auditService;

    public VolunteerService(SystemMapper systemMapper, SensitiveDataService sensitiveData,
                            AuditService auditService) {
        this.systemMapper = systemMapper;
        this.sensitiveData = sensitiveData;
        this.auditService = auditService;
    }

    public ApiModels.VolunteerView me() {
        return toView(requireProfile(SecurityUtils.current().id()), false);
    }

    @Transactional
    public ApiModels.VolunteerView update(ApiModels.VolunteerProfileRequest request) {
        Long userId = SecurityUtils.current().id();
        Domain.VolunteerRow current = requireProfile(userId);
        if ("PENDING_REVIEW".equals(current.certificationStatus())) {
            throw BusinessException.conflict("资料审核中，暂不能修改");
        }
        if ("SUSPENDED".equals(current.certificationStatus())) {
            throw BusinessException.forbidden("认证已暂停，请联系管理员");
        }
        BusinessRules.requireUpdated(systemMapper.updateVolunteer(userId,
                sensitiveData.encrypt(request.realName().trim()), sensitiveData.encrypt(request.phone()),
                request.serviceRegion().trim(), request.skills().trim(), request.availableTime().trim(),
                request.introduction().trim()));
        auditService.log("UPDATE_VOLUNTEER", "VOLUNTEER", userId,
                current.certificationStatus(), "UNVERIFIED", "更新志愿者认证资料");
        return toView(requireProfile(userId), false);
    }

    @Transactional
    public void submit() {
        Long userId = SecurityUtils.current().id();
        Domain.VolunteerRow current = requireProfile(userId);
        BusinessRules.requireState(current.certificationStatus(), "UNVERIFIED", "REJECTED");
        BusinessRules.requireUpdated(systemMapper.submitVolunteer(userId));
        auditService.log("SUBMIT_VOLUNTEER", "VOLUNTEER", userId,
                current.certificationStatus(), "PENDING_REVIEW", "提交志愿者认证");
    }

    public ApiModels.VolunteerView toView(Domain.VolunteerRow row, boolean masked) {
        String name = sensitiveData.decrypt(row.realNameEncrypted());
        String phone = sensitiveData.decrypt(row.phoneEncrypted());
        return new ApiModels.VolunteerView(row.userId(), row.username(), row.displayName(),
                masked ? sensitiveData.maskName(name) : name, masked ? sensitiveData.maskPhone(phone) : phone,
                row.serviceRegion(), row.skills(), row.availableTime(), row.introduction(),
                row.certificationStatus(), row.rejectionReason(), row.updatedAt());
    }

    private Domain.VolunteerRow requireProfile(Long userId) {
        Domain.VolunteerRow row = systemMapper.findVolunteer(userId);
        if (row == null) {
            throw BusinessException.notFound("志愿者资料不存在");
        }
        return row;
    }
}
