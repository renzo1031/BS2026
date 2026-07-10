package com.campus.service.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.campus.service.common.AuthContext;
import com.campus.service.common.BusinessException;
import com.campus.service.common.JwtUtil;
import com.campus.service.entity.Role;
import com.campus.service.entity.StudentIdentity;
import com.campus.service.entity.User;
import com.campus.service.entity.UserRole;
import com.campus.service.mapper.RoleMapper;
import com.campus.service.mapper.StudentIdentityMapper;
import com.campus.service.mapper.UserMapper;
import com.campus.service.mapper.UserRoleMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthService {
    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final StudentIdentityMapper identityMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final LoginAttemptService loginAttempts;
    private final LogService logService;

    public AuthService(UserMapper userMapper, RoleMapper roleMapper, UserRoleMapper userRoleMapper,
                       StudentIdentityMapper identityMapper, PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil, LoginAttemptService loginAttempts, LogService logService) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.identityMapper = identityMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.loginAttempts = loginAttempts;
        this.logService = logService;
    }

    @Transactional
    public UserView register(RegisterCommand command) {
        if (userMapper.findByUsername(command.username()) != null) {
            throw new BusinessException(409, "用户名已存在");
        }
        if (userMapper.findByStudentNo(command.studentNo()) != null) {
            throw new BusinessException(409, "该学号已绑定账号");
        }
        StudentIdentity identity = identityMapper.findByStudentNoForUpdate(command.studentNo());
        if (identity == null || !"ACTIVE".equals(identity.getStatus()) || identity.getBoundUserId() != null
                || !identity.getRealName().equals(command.realName().trim())) {
            throw new BusinessException(400, "学生身份信息校验失败");
        }
        Role role = roleMapper.selectOne(new QueryWrapper<Role>().eq("code", "STUDENT"));
        if (role == null) {
            throw new BusinessException(500, "系统缺少学生角色初始化数据");
        }
        LocalDateTime now = LocalDateTime.now();
        User user = new User();
        user.setUsername(command.username().trim());
        user.setPasswordHash(passwordEncoder.encode(command.password()));
        user.setRealName(identity.getRealName());
        user.setStudentNo(identity.getStudentNo());
        user.setPhone(blankToNull(command.phone()));
        user.setEmail(blankToNull(command.email()));
        user.setCollege(identity.getCollege());
        user.setMajor(identity.getMajor());
        user.setStatus("ENABLED");
        user.setTokenVersion(0);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        user.setDeleted(0);
        userMapper.insert(user);

        UserRole userRole = new UserRole();
        userRole.setUserId(user.getId());
        userRole.setRoleId(role.getId());
        userRoleMapper.insert(userRole);
        identity.setBoundUserId(user.getId());
        identityMapper.updateById(identity);
        logService.log("AUTH", "REGISTER", user.getUsername());
        return toView(user, Set.of("STUDENT"));
    }

    public LoginResult login(LoginCommand command) {
        loginAttempts.checkAllowed(command.username());
        User user = userMapper.findByUsername(command.username());
        if (user == null || !passwordEncoder.matches(command.password(), user.getPasswordHash())
                || !"ENABLED".equals(user.getStatus())) {
            loginAttempts.recordFailure(command.username());
            logService.log("AUTH", "LOGIN_FAILED", safeUsername(command.username()));
            throw new BusinessException(401, "用户名或密码错误");
        }
        loginAttempts.recordSuccess(command.username());
        Set<String> roles = roleMapper.findByUserId(user.getId()).stream()
                .map(Role::getCode)
                .collect(Collectors.toSet());
        String token = jwtUtil.create(user.getId(), user.getUsername(), roles, user.getTokenVersion());
        logService.log("AUTH", "LOGIN", user.getUsername());
        return new LoginResult(token, toView(user, roles));
    }

    public UserView me() {
        AuthContext.AuthUser auth = AuthContext.get();
        User user = requireCurrentUser();
        return toView(user, auth.roles());
    }

    @Transactional
    public void logout() {
        User user = requireCurrentUser();
        user.setTokenVersion(user.getTokenVersion() + 1);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        logService.log("AUTH", "LOGOUT", user.getUsername());
    }

    public UserView updateProfile(UpdateProfileCommand command) {
        User user = requireCurrentUser();
        user.setPhone(blankToNull(command.phone()));
        user.setEmail(blankToNull(command.email()));
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        logService.log("USER", "UPDATE_PROFILE", user.getUsername());
        return toView(user, AuthContext.get().roles());
    }

    private User requireCurrentUser() {
        User user = userMapper.selectById(AuthContext.get().userId());
        if (user == null || user.getDeleted() == 1) {
            throw new BusinessException(401, "登录状态已失效，请重新登录");
        }
        return user;
    }

    private UserView toView(User user, Set<String> roles) {
        return new UserView(user.getId(), user.getUsername(), user.getRealName(), user.getStudentNo(),
                user.getPhone(), user.getEmail(), user.getCollege(), user.getMajor(),
                user.getDepartmentId(), List.copyOf(roles));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String safeUsername(String value) {
        return value == null ? "" : value.replace("\r", "").replace("\n", "").trim();
    }

    public record RegisterCommand(String username, String password, String realName, String studentNo,
                                  String phone, String email) {
    }

    public record LoginCommand(String username, String password) {
    }

    public record UpdateProfileCommand(String phone, String email) {
    }

    public record LoginResult(String token, UserView user) {
    }

    public record UserView(Long id, String username, String realName, String studentNo, String phone,
                           String email, String college, String major, Long departmentId, List<String> roles) {
    }
}
