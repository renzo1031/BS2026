package com.campus.service.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.service.common.AuthContext;
import com.campus.service.common.BusinessException;
import com.campus.service.common.PageResult;
import com.campus.service.entity.*;
import com.campus.service.mapper.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AdminService {
    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final DepartmentMapper departmentMapper;
    private final ServiceItemMapper itemMapper;
    private final VenueMapper venueMapper;
    private final OperationLogMapper logMapper;
    private final PasswordEncoder passwordEncoder;
    private final LogService logService;

    public AdminService(UserMapper userMapper, RoleMapper roleMapper, UserRoleMapper userRoleMapper,
                        DepartmentMapper departmentMapper, ServiceItemMapper itemMapper, VenueMapper venueMapper,
                        OperationLogMapper logMapper, PasswordEncoder passwordEncoder, LogService logService) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.departmentMapper = departmentMapper;
        this.itemMapper = itemMapper;
        this.venueMapper = venueMapper;
        this.logMapper = logMapper;
        this.passwordEncoder = passwordEncoder;
        this.logService = logService;
    }

    public PageResult<UserSummary> users(long page, long size, String keyword) {
        QueryWrapper<User> query = new QueryWrapper<User>().eq("deleted", 0).orderByDesc("created_at");
        if (keyword != null && !keyword.isBlank()) {
            query.and(q -> q.like("username", keyword.trim()).or().like("real_name", keyword.trim())
                    .or().like("student_no", keyword.trim()));
        }
        Page<User> result = userMapper.selectPage(Page.of(page, size), query);
        List<UserSummary> records = result.getRecords().stream().map(this::summary).toList();
        return new PageResult<>(result.getTotal(), result.getCurrent(), result.getSize(), records);
    }

    @Transactional
    public UserSummary createStaff(CreateStaffCommand command) {
        if (userMapper.findByUsername(command.username()) != null) {
            throw new BusinessException(409, "用户名已存在");
        }
        Department department = departmentMapper.selectById(command.departmentId());
        if (department == null) {
            throw new BusinessException(404, "部门不存在");
        }
        Role staffRole = roleMapper.selectOne(new QueryWrapper<Role>().eq("code", "STAFF"));
        if (staffRole == null) {
            throw new BusinessException(500, "系统缺少部门人员角色初始化数据");
        }
        LocalDateTime now = LocalDateTime.now();
        User user = new User();
        user.setUsername(command.username().trim());
        user.setPasswordHash(passwordEncoder.encode(command.password()));
        user.setRealName(command.realName().trim());
        user.setPhone(blankToNull(command.phone()));
        user.setEmail(blankToNull(command.email()));
        user.setDepartmentId(department.getId());
        user.setStatus("ENABLED");
        user.setTokenVersion(0);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        user.setDeleted(0);
        userMapper.insert(user);

        UserRole link = new UserRole();
        link.setUserId(user.getId());
        link.setRoleId(staffRole.getId());
        userRoleMapper.insert(link);
        logService.log("ADMIN", "CREATE_STAFF", user.getUsername() + "@" + department.getName());
        return summary(user);
    }

    @Transactional
    public void updateUserStatus(Long id, String status) {
        if (!List.of("ENABLED", "DISABLED").contains(status)) {
            throw new BusinessException("账号状态不正确");
        }
        User user = userMapper.selectById(id);
        if (user == null || user.getDeleted() == 1) {
            throw new BusinessException(404, "用户不存在");
        }
        if (id.equals(AuthContext.get().userId()) && "DISABLED".equals(status)) {
            throw new BusinessException("不能停用当前登录账号");
        }
        if (!status.equals(user.getStatus())) {
            user.setStatus(status);
            user.setTokenVersion(user.getTokenVersion() + 1);
            user.setUpdatedAt(LocalDateTime.now());
            userMapper.updateById(user);
            logService.log("ADMIN", "UPDATE_USER_STATUS", user.getUsername() + " -> " + status);
        }
    }

    public List<Department> departments() {
        return departmentMapper.selectList(new QueryWrapper<Department>().orderByAsc("id"));
    }

    public List<ServiceItemView> serviceItems() {
        List<ServiceItem> items = itemMapper.selectList(new QueryWrapper<ServiceItem>().orderByAsc("id"));
        if (items.isEmpty()) {
            return List.of();
        }
        var departments = departmentMapper.selectByIds(items.stream()
                        .map(ServiceItem::getDepartmentId).distinct().toList()).stream()
                .collect(java.util.stream.Collectors.toMap(Department::getId, Department::getName));
        return items.stream().map(item -> new ServiceItemView(item.getId(), item.getCategoryId(),
                item.getDepartmentId(), departments.get(item.getDepartmentId()), item.getCode(), item.getName(),
                item.getType(), item.getDescription(), item.getRequiredMaterials(), item.getNeedVenue(),
                item.getEnabled())).toList();
    }

    public void updateServiceItemEnabled(Long id, Boolean enabled) {
        ServiceItem item = itemMapper.selectById(id);
        if (item == null) {
            throw new BusinessException(404, "服务事项不存在");
        }
        item.setEnabled(Boolean.TRUE.equals(enabled) ? 1 : 0);
        itemMapper.updateById(item);
        logService.log("ADMIN", "UPDATE_SERVICE_ITEM", item.getCode() + " enabled=" + item.getEnabled());
    }

    public List<Venue> venues() {
        return venueMapper.selectList(new QueryWrapper<Venue>().orderByAsc("id"));
    }

    public void updateVenueStatus(Long id, String status) {
        if (!List.of("AVAILABLE", "UNAVAILABLE").contains(status)) {
            throw new BusinessException("场地状态不正确");
        }
        Venue venue = venueMapper.selectById(id);
        if (venue == null) {
            throw new BusinessException(404, "场地不存在");
        }
        venue.setStatus(status);
        venueMapper.updateById(venue);
        logService.log("ADMIN", "UPDATE_VENUE", venue.getName() + " -> " + status);
    }

    public PageResult<OperationLog> logs(long page, long size) {
        Page<OperationLog> result = logMapper.selectPage(Page.of(page, size),
                new QueryWrapper<OperationLog>().orderByDesc("created_at"));
        return new PageResult<>(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords());
    }

    private UserSummary summary(User user) {
        return new UserSummary(user.getId(), user.getUsername(), user.getRealName(), user.getStudentNo(),
                user.getPhone(), user.getEmail(), user.getCollege(), user.getMajor(),
                user.getDepartmentId(), user.getStatus(), user.getCreatedAt());
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public record CreateStaffCommand(String username, String password, String realName, Long departmentId,
                                     String phone, String email) {
    }

    public record UserSummary(Long id, String username, String realName, String studentNo, String phone,
                              String email, String college, String major, Long departmentId,
                              String status, LocalDateTime createdAt) {
    }

    public record ServiceItemView(Long id, Long categoryId, Long departmentId, String departmentName,
                                  String code, String name, String type, String description,
                                  String requiredMaterials, Integer needVenue, Integer enabled) {
    }
}
