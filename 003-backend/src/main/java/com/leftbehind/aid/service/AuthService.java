package com.leftbehind.aid.service;

import com.leftbehind.aid.api.ApiModels;
import com.leftbehind.aid.common.BusinessException;
import com.leftbehind.aid.domain.Domain;
import com.leftbehind.aid.mapper.SystemMapper;
import com.leftbehind.aid.security.JwtService;
import com.leftbehind.aid.security.PlatformPrincipal;
import com.leftbehind.aid.security.SecurityUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class AuthService {
    private static final String DUMMY_HASH = "$2a$12$wBM.3eqWH8n2otnIgNfQ6u.N1ltHCZmm8d3NtUfV7h60lqOXZ9ypa";
    private final SystemMapper systemMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditService auditService;

    public AuthService(SystemMapper systemMapper, PasswordEncoder passwordEncoder,
                       JwtService jwtService, AuditService auditService) {
        this.systemMapper = systemMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.auditService = auditService;
    }

    @Transactional
    public void register(ApiModels.RegisterRequest request) {
        String username = request.username().trim().toLowerCase(Locale.ROOT);
        Long roleId = systemMapper.findRoleIdByCode(Domain.RoleCode.VOLUNTEER.name());
        try {
            systemMapper.insertUser(null, roleId, username, passwordEncoder.encode(request.password()), request.displayName().trim());
        } catch (DuplicateKeyException exception) {
            throw BusinessException.conflict("用户名已被使用");
        }
        Domain.UserRow user = systemMapper.findUserByUsername(username);
        systemMapper.insertVolunteerProfile(user.id());
        auditService.log(user.id(), "REGISTER", "USER", user.id(), null, "ACTIVE", "志愿者自主注册");
    }

    @Transactional(noRollbackFor = BusinessException.class)
    public ApiModels.LoginResponse login(ApiModels.LoginRequest request) {
        String username = request.username().trim().toLowerCase(Locale.ROOT);
        Domain.UserRow user = systemMapper.findUserByUsername(username);
        if (user == null) {
            passwordEncoder.matches(request.password(), DUMMY_HASH);
            throw invalidCredentials();
        }
        if (!"ACTIVE".equals(user.status())) {
            passwordEncoder.matches(request.password(), user.passwordHash());
            throw invalidCredentials();
        }
        LocalDateTime now = LocalDateTime.now();
        if (user.lockedUntil() != null && user.lockedUntil().isAfter(now)) {
            throw invalidCredentials();
        }
        if (user.lockedUntil() != null) {
            systemMapper.resetLoginFailures(user.id());
        }
        if (!passwordEncoder.matches(request.password(), user.passwordHash())) {
            systemMapper.recordLoginFailure(user.id());
            throw invalidCredentials();
        }
        systemMapper.recordLoginSuccess(user.id());
        String sessionId = UUID.randomUUID().toString();
        JwtService.Token token = jwtService.issue(user.id(), sessionId);
        systemMapper.insertSession(sessionId, user.id(), token.expiresAt());
        auditService.log(user.id(), "LOGIN", "AUTH_SESSION", sessionId, null, "ACTIVE", "登录成功");
        return new ApiModels.LoginResponse(token.value(), token.expiresAt(), toMe(user, sessionId));
    }

    @Transactional
    public void logout() {
        PlatformPrincipal principal = SecurityUtils.current();
        systemMapper.revokeSession(principal.sessionId(), principal.id());
        auditService.log(principal.id(), "LOGOUT", "AUTH_SESSION", principal.sessionId(), "ACTIVE", "REVOKED", "用户退出登录");
    }

    public ApiModels.MeView me() {
        PlatformPrincipal principal = SecurityUtils.current();
        return new ApiModels.MeView(principal.id(), principal.departmentId(), principal.username(),
                principal.displayName(), principal.roleCode(), principal.dataScope(), principal.permissions());
    }

    @Transactional
    public void changePassword(ApiModels.PasswordChangeRequest request) {
        PlatformPrincipal principal = SecurityUtils.current();
        Domain.UserRow user = systemMapper.findUserById(principal.id());
        if (user == null || !passwordEncoder.matches(request.oldPassword(), user.passwordHash())) {
            throw BusinessException.badRequest("原密码不正确");
        }
        if (passwordEncoder.matches(request.newPassword(), user.passwordHash())) {
            throw BusinessException.badRequest("新密码不能与原密码相同");
        }
        systemMapper.updatePassword(user.id(), passwordEncoder.encode(request.newPassword()));
        systemMapper.revokeUserSessions(user.id());
        auditService.log(user.id(), "CHANGE_PASSWORD", "USER", user.id(), null, null, "修改密码并撤销全部会话");
    }

    private ApiModels.MeView toMe(Domain.UserRow user, String sessionId) {
        Set<String> permissions = Set.copyOf(new LinkedHashSet<>(systemMapper.findPermissionCodes(user.roleId())));
        return new ApiModels.MeView(user.id(), user.departmentId(), user.username(), user.displayName(),
                user.roleCode(), user.dataScope(), permissions);
    }

    private BusinessException invalidCredentials() {
        return BusinessException.unauthorized("用户名或密码错误，连续失败可能导致账号临时锁定");
    }
}
