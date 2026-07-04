package com.campus.lostfound.system.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.lostfound.common.BizException;
import com.campus.lostfound.common.IdGenerator;
import com.campus.lostfound.common.MapReader;
import com.campus.lostfound.security.CurrentUser;
import com.campus.lostfound.security.JwtUtil;
import com.campus.lostfound.security.LoginContext;
import com.campus.lostfound.security.PasswordService;
import com.campus.lostfound.system.user.entity.SysRole;
import com.campus.lostfound.system.user.entity.SysUser;
import com.campus.lostfound.system.user.entity.SysUserRole;
import com.campus.lostfound.system.user.mapper.SysRoleMapper;
import com.campus.lostfound.system.user.mapper.SysUserMapper;
import com.campus.lostfound.system.user.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final PasswordService passwordService;
    private final JwtUtil jwtUtil;

    @Transactional
    public Map<String, Object> register(Map<String, Object> body) {
        String username = MapReader.requiredStr(body, "username", "用户名");
        String password = MapReader.requiredStr(body, "password", "密码");
        String realName = MapReader.requiredStr(body, "realName", "姓名");
        String phone = MapReader.requiredStr(body, "phone", "手机号");
        String studentNo = MapReader.str(body, "studentNo");

        if (userMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username)) > 0) {
            throw new BizException("用户名已存在");
        }
        if (userMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getPhone, phone)) > 0) {
            throw new BizException("手机号已存在");
        }

        SysUser user = new SysUser();
        user.setId(IdGenerator.nextId());
        user.setUsername(username);
        user.setPasswordHash(passwordService.hash(password));
        user.setRealName(realName);
        user.setPhone(phone);
        user.setStudentNo(studentNo);
        user.setEmail(MapReader.str(body, "email"));
        user.setStatus("ENABLED");
        user.setCreatedBy(user.getId());
        user.setUpdatedBy(user.getId());
        userMapper.insert(user);

        SysRole role = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleCode, "USER"));
        if (role == null) {
            throw new BizException("普通用户角色不存在，请先初始化数据库");
        }
        SysUserRole userRole = new SysUserRole();
        userRole.setId(IdGenerator.nextId());
        userRole.setUserId(user.getId());
        userRole.setRoleId(role.getId());
        userRole.setCreatedBy(user.getId());
        userRole.setUpdatedBy(user.getId());
        userRoleMapper.insert(userRole);

        return loginUser(user);
    }

    public Map<String, Object> login(Map<String, Object> body) {
        String username = MapReader.requiredStr(body, "username", "用户名");
        String password = MapReader.requiredStr(body, "password", "密码");
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        if (user == null || !"ENABLED".equals(user.getStatus())) {
            throw new BizException("账号不存在或已禁用");
        }
        if (!passwordService.matches(password, user.getPasswordHash())) {
            throw new BizException("用户名或密码错误");
        }
        user.setLastLoginTime(LocalDateTime.now());
        userMapper.updateById(user);
        return loginUser(user);
    }

    public Map<String, Object> me() {
        CurrentUser current = LoginContext.get();
        SysUser user = userMapper.selectById(current.id());
        Map<String, Object> result = new HashMap<>();
        result.put("user", user);
        result.put("roles", current.roles());
        return result;
    }

    public SysUser updateProfile(Map<String, Object> body) {
        CurrentUser current = LoginContext.get();
        SysUser user = userMapper.selectById(current.id());
        user.setRealName(MapReader.requiredStr(body, "realName", "姓名"));
        user.setPhone(MapReader.requiredStr(body, "phone", "手机号"));
        user.setStudentNo(MapReader.str(body, "studentNo"));
        user.setEmail(MapReader.str(body, "email"));
        user.setUpdatedBy(current.id());
        userMapper.updateById(user);
        return user;
    }

    public void changePassword(Map<String, Object> body) {
        CurrentUser current = LoginContext.get();
        String oldPassword = MapReader.requiredStr(body, "oldPassword", "原密码");
        String newPassword = MapReader.requiredStr(body, "newPassword", "新密码");
        SysUser user = userMapper.selectById(current.id());
        if (!passwordService.matches(oldPassword, user.getPasswordHash())) {
            throw new BizException("原密码错误");
        }
        user.setPasswordHash(passwordService.hash(newPassword));
        user.setUpdatedBy(current.id());
        userMapper.updateById(user);
    }

    private Map<String, Object> loginUser(SysUser user) {
        Set<String> roles = loadRoleCodes(user.getId());
        CurrentUser currentUser = new CurrentUser(user.getId(), user.getUsername(), user.getRealName(), roles);
        String token = jwtUtil.createToken(currentUser);
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("user", user);
        result.put("roles", roles);
        return result;
    }

    private Set<String> loadRoleCodes(Long userId) {
        List<SysUserRole> userRoles = userRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
        List<Long> roleIds = userRoles.stream().map(SysUserRole::getRoleId).toList();
        if (roleIds.isEmpty()) {
            return Set.of("USER");
        }
        return roleMapper.selectBatchIds(roleIds).stream().map(SysRole::getRoleCode).collect(Collectors.toSet());
    }
}
