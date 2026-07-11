package com.leftbehind.aid.service;

import com.leftbehind.aid.api.ApiModels;
import com.leftbehind.aid.common.BusinessException;
import com.leftbehind.aid.common.PageResult;
import com.leftbehind.aid.domain.Domain;
import com.leftbehind.aid.mapper.SystemMapper;
import com.leftbehind.aid.security.SecurityUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class AdminService {
    private final SystemMapper systemMapper;
    private final VolunteerService volunteerService;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public AdminService(SystemMapper systemMapper, VolunteerService volunteerService,
                        PasswordEncoder passwordEncoder, AuditService auditService) {
        this.systemMapper = systemMapper;
        this.volunteerService = volunteerService;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    public PageResult<ApiModels.UserAdminView> users(int page, int size, String keyword, String roleCode) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        List<ApiModels.UserAdminView> items = systemMapper.listUsers(clean(keyword), clean(roleCode),
                        (safePage - 1) * safeSize, safeSize).stream()
                .map(row -> new ApiModels.UserAdminView(row.id(), row.username(), row.displayName(), row.roleCode(),
                        row.roleName(), row.departmentId(), row.departmentName(), row.status(),
                        row.lastLoginAt(), row.createdAt())).toList();
        return new PageResult<>(items, systemMapper.countUsers(clean(keyword), clean(roleCode)), safePage, safeSize);
    }

    @Transactional
    public void createUser(ApiModels.AdminCreateUserRequest request) {
        Domain.RoleRow role = systemMapper.findRole(request.roleId());
        if (role == null) {
            throw BusinessException.badRequest("角色不存在");
        }
        Long departmentId = request.departmentId();
        if (("SUPERVISOR".equals(role.code()) || "CASE_WORKER".equals(role.code())) && departmentId == null) {
            throw BusinessException.badRequest("业务人员必须关联部门");
        }
        if ("SYS_ADMIN".equals(role.code()) || "VOLUNTEER".equals(role.code())) {
            departmentId = null;
        }
        String username = request.username().trim().toLowerCase(Locale.ROOT);
        try {
            systemMapper.insertUser(departmentId, role.id(), username,
                    passwordEncoder.encode(request.password()), request.displayName().trim());
        } catch (DuplicateKeyException exception) {
            throw BusinessException.conflict("用户名已被使用");
        }
        Domain.UserRow created = systemMapper.findUserByUsername(username);
        if ("VOLUNTEER".equals(role.code())) {
            systemMapper.insertVolunteerProfile(created.id());
        }
        auditService.log("CREATE_USER", "USER", created.id(), null, "ACTIVE", "管理员创建账号");
    }

    @Transactional
    public void updateUserStatus(Long id, String status) {
        if (id.equals(SecurityUtils.current().id()) && "DISABLED".equals(status)) {
            throw BusinessException.badRequest("不能停用当前登录账号");
        }
        Domain.UserRow user = systemMapper.findUserById(id);
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }
        BusinessRules.requireUpdated(systemMapper.updateUserStatus(id, status));
        if ("DISABLED".equals(status)) {
            systemMapper.revokeUserSessions(id);
        }
        auditService.log("UPDATE_USER_STATUS", "USER", id, user.status(), status, "管理员修改账号状态");
    }

    public List<Domain.DepartmentRow> departments() {
        return systemMapper.listDepartments();
    }

    @Transactional
    public void createDepartment(ApiModels.DepartmentRequest request) {
        try {
            systemMapper.insertDepartment(request.code(), request.name().trim());
        } catch (DuplicateKeyException exception) {
            throw BusinessException.conflict("部门编码已存在");
        }
        auditService.log("CREATE_DEPARTMENT", "DEPARTMENT", request.code(), null, "ACTIVE", "创建业务部门");
    }

    public List<ApiModels.RoleView> roles() {
        return systemMapper.listRoles().stream().map(role -> new ApiModels.RoleView(
                role.id(), role.code(), role.name(), role.dataScope(), systemMapper.findPermissionIds(role.id()))).toList();
    }

    public List<Domain.PermissionRow> permissions() {
        return systemMapper.listPermissions();
    }

    @Transactional
    public void updateRolePermissions(Long roleId, ApiModels.RolePermissionRequest request) {
        Domain.RoleRow role = systemMapper.findRole(roleId);
        if (role == null) {
            throw BusinessException.notFound("角色不存在");
        }
        if ("SYS_ADMIN".equals(role.code())) {
            throw BusinessException.forbidden("系统管理员权限为安全基线，不允许在线修改");
        }
        for (Long permissionId : request.permissionIds()) {
            if (systemMapper.permissionExists(permissionId) != 1) {
                throw BusinessException.badRequest("包含不存在的权限");
            }
        }
        systemMapper.deleteRolePermissions(roleId);
        request.permissionIds().forEach(permissionId -> systemMapper.insertRolePermission(roleId, permissionId));
        auditService.log("UPDATE_ROLE_PERMISSIONS", "ROLE", roleId, null, null, "更新角色权限");
    }

    public PageResult<ApiModels.VolunteerView> volunteers(int page, int size, String status) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        List<ApiModels.VolunteerView> items = systemMapper.listVolunteers(clean(status),
                        (safePage - 1) * safeSize, safeSize).stream()
                .map(row -> volunteerService.toView(row, false)).toList();
        return new PageResult<>(items, systemMapper.countVolunteers(clean(status)), safePage, safeSize);
    }

    @Transactional
    public void reviewVolunteer(Long userId, ApiModels.DecisionRequest request) {
        String rejectionReason = BusinessRules.rejectionReason(request.decision(), request.comment());
        Domain.VolunteerRow volunteer = systemMapper.findVolunteer(userId);
        if (volunteer == null) {
            throw BusinessException.notFound("志愿者资料不存在");
        }
        BusinessRules.requireState(volunteer.certificationStatus(), "PENDING_REVIEW");
        BusinessRules.requireUpdated(systemMapper.reviewVolunteer(userId, request.decision(),
                rejectionReason, SecurityUtils.current().id()));
        auditService.log("REVIEW_VOLUNTEER", "VOLUNTEER", userId,
                volunteer.certificationStatus(), request.decision(), "审核志愿者认证");
    }

    public PageResult<Domain.AuditRow> auditLogs(int page, int size, String businessType) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        return new PageResult<>(systemMapper.listAuditLogs(clean(businessType), (safePage - 1) * safeSize, safeSize),
                systemMapper.countAuditLogs(clean(businessType)), safePage, safeSize);
    }

    private static String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
