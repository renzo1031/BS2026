package com.campus.lostfound.system.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.lostfound.common.BizException;
import com.campus.lostfound.common.IdGenerator;
import com.campus.lostfound.common.MapReader;
import com.campus.lostfound.common.PageResult;
import com.campus.lostfound.lostfound.category.entity.LfCategory;
import com.campus.lostfound.lostfound.category.mapper.LfCategoryMapper;
import com.campus.lostfound.lostfound.claim.entity.LfClaimApplication;
import com.campus.lostfound.lostfound.claim.mapper.LfClaimApplicationMapper;
import com.campus.lostfound.lostfound.location.entity.LfLocation;
import com.campus.lostfound.lostfound.location.mapper.LfLocationMapper;
import com.campus.lostfound.lostfound.item.entity.LfItem;
import com.campus.lostfound.lostfound.item.mapper.LfItemMapper;
import com.campus.lostfound.security.LoginContext;
import com.campus.lostfound.security.PasswordService;
import com.campus.lostfound.security.PermissionGuard;
import com.campus.lostfound.system.log.service.OperationLogService;
import com.campus.lostfound.system.log.entity.SysOperationLog;
import com.campus.lostfound.system.log.mapper.SysOperationLogMapper;
import com.campus.lostfound.system.notice.entity.SysNotice;
import com.campus.lostfound.system.notice.mapper.SysNoticeMapper;
import com.campus.lostfound.system.user.entity.SysUser;
import com.campus.lostfound.system.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminService {
    private final SysUserMapper userMapper;
    private final LfCategoryMapper categoryMapper;
    private final LfLocationMapper locationMapper;
    private final SysNoticeMapper noticeMapper;
    private final SysOperationLogMapper logMapper;
    private final LfItemMapper itemMapper;
    private final LfClaimApplicationMapper claimMapper;
    private final PasswordService passwordService;
    private final OperationLogService operationLogService;

    public PageResult<SysUser> users(Map<String, String> query) {
        requireAdmin();
        QueryWrapper<SysUser> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted", 0);
        if (query.get("keyword") != null && !query.get("keyword").isBlank()) {
            wrapper.and(w -> w.like("username", query.get("keyword")).or().like("real_name", query.get("keyword")).or().like("phone", query.get("keyword")));
        }
        wrapper.orderByDesc("created_at");
        return PageResult.from(userMapper.selectPage(userPage(query), wrapper));
    }

    public void updateUserStatus(Long id, Map<String, Object> body) {
        requireAdmin();
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        user.setStatus(MapReader.requiredStr(body, "status", "状态"));
        user.setUpdatedBy(LoginContext.get().id());
        userMapper.updateById(user);
    }

    public void resetUserPassword(Long id, Map<String, Object> body) {
        requireAdmin();
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        String newPassword = MapReader.requiredStr(body, "newPassword", "新密码");
        if (newPassword.length() < 6) {
            throw new BizException("新密码长度不能少于 6 位");
        }
        user.setPasswordHash(passwordService.hash(newPassword));
        user.setUpdatedBy(LoginContext.get().id());
        userMapper.updateById(user);
        operationLogService.record("USER", id, "PASSWORD_RESET", null, null, "管理员重置用户密码");
    }

    public PageResult<LfCategory> categories(Map<String, String> query) {
        requireAdmin();
        QueryWrapper<LfCategory> wrapper = new QueryWrapper<LfCategory>().eq("deleted", 0).orderByAsc("sort_order");
        return PageResult.from(categoryMapper.selectPage(categoryPage(query), wrapper));
    }

    public LfCategory saveCategory(Map<String, Object> body) {
        requireAdmin();
        LfCategory category = new LfCategory();
        Long id = MapReader.longValue(body, "id");
        category.setId(id == null ? IdGenerator.nextId() : id);
        category.setCategoryName(MapReader.requiredStr(body, "categoryName", "分类名称"));
        category.setSortOrder(MapReader.longValue(body, "sortOrder") == null ? 0 : MapReader.longValue(body, "sortOrder").intValue());
        category.setStatus(MapReader.str(body, "status") == null ? "ENABLED" : MapReader.str(body, "status"));
        category.setUpdatedBy(LoginContext.get().id());
        if (id == null) {
            category.setCreatedBy(LoginContext.get().id());
            categoryMapper.insert(category);
        } else {
            categoryMapper.updateById(category);
        }
        return category;
    }

    public PageResult<LfLocation> locations(Map<String, String> query) {
        requireAdmin();
        QueryWrapper<LfLocation> wrapper = new QueryWrapper<LfLocation>().eq("deleted", 0).orderByAsc("sort_order");
        return PageResult.from(locationMapper.selectPage(locationPage(query), wrapper));
    }

    public LfLocation saveLocation(Map<String, Object> body) {
        requireAdmin();
        LfLocation location = new LfLocation();
        Long id = MapReader.longValue(body, "id");
        location.setId(id == null ? IdGenerator.nextId() : id);
        location.setLocationName(MapReader.requiredStr(body, "locationName", "地点名称"));
        location.setAreaName(MapReader.str(body, "areaName"));
        location.setSortOrder(MapReader.longValue(body, "sortOrder") == null ? 0 : MapReader.longValue(body, "sortOrder").intValue());
        location.setStatus(MapReader.str(body, "status") == null ? "ENABLED" : MapReader.str(body, "status"));
        location.setUpdatedBy(LoginContext.get().id());
        if (id == null) {
            location.setCreatedBy(LoginContext.get().id());
            locationMapper.insert(location);
        } else {
            locationMapper.updateById(location);
        }
        return location;
    }

    public PageResult<SysNotice> notices(Map<String, String> query) {
        requireAdmin();
        QueryWrapper<SysNotice> wrapper = new QueryWrapper<SysNotice>().eq("deleted", 0).orderByDesc("created_at");
        return PageResult.from(noticeMapper.selectPage(noticePage(query), wrapper));
    }

    public SysNotice saveNotice(Map<String, Object> body) {
        requireAdmin();
        Long id = MapReader.longValue(body, "id");
        SysNotice notice = id == null ? new SysNotice() : noticeMapper.selectById(id);
        if (notice == null) {
            throw new BizException("公告不存在");
        }
        notice.setId(id == null ? IdGenerator.nextId() : id);
        notice.setNoticeType(blankDefault(MapReader.str(body, "noticeType"), "ANNOUNCEMENT"));
        notice.setTitle(MapReader.requiredStr(body, "title", "标题"));
        notice.setContent(MapReader.requiredStr(body, "content", "内容"));
        notice.setReceiverId(MapReader.longValue(body, "receiverId"));
        notice.setReadStatus(blankDefault(MapReader.str(body, "readStatus"), "UNREAD"));
        notice.setPublishStatus(blankDefault(MapReader.str(body, "publishStatus"), "PUBLISHED"));
        notice.setStartTime(MapReader.dateTime(body, "startTime"));
        notice.setEndTime(MapReader.dateTime(body, "endTime"));
        if (notice.getStartTime() != null && notice.getEndTime() != null && notice.getEndTime().isBefore(notice.getStartTime())) {
            throw new BizException("结束时间不能早于开始时间");
        }
        notice.setPopupEnabled(booleanFlag(body, "popupEnabled"));
        if (notice.getPublishedAt() == null && "PUBLISHED".equals(notice.getPublishStatus())) {
            notice.setPublishedAt(LocalDateTime.now());
        }
        notice.setUpdatedBy(LoginContext.get().id());
        if (id == null) {
            notice.setCreatedBy(LoginContext.get().id());
            noticeMapper.insert(notice);
        } else {
            noticeMapper.updateById(notice);
        }
        return notice;
    }

    public void deleteNotice(Long id) {
        requireAdmin();
        SysNotice notice = noticeMapper.selectById(id);
        if (notice == null) {
            throw new BizException("公告不存在");
        }
        notice.setUpdatedBy(LoginContext.get().id());
        noticeMapper.updateById(notice);
        noticeMapper.deleteById(id);
    }

    public PageResult<SysOperationLog> logs(Map<String, String> query) {
        requireAdmin();
        QueryWrapper<SysOperationLog> wrapper = new QueryWrapper<>();
        if (query.get("targetType") != null && !query.get("targetType").isBlank()) {
            wrapper.eq("target_type", query.get("targetType"));
        }
        if (query.get("action") != null && !query.get("action").isBlank()) {
            wrapper.eq("action", query.get("action"));
        }
        if (query.get("operatorId") != null && !query.get("operatorId").isBlank()) {
            wrapper.eq("operator_id", query.get("operatorId"));
        }
        wrapper.orderByDesc("created_at");
        return PageResult.from(logMapper.selectPage(logPage(query), wrapper));
    }

    public Map<String, Object> statistics() {
        requireAdmin();
        Map<String, Object> stats = new HashMap<>();
        stats.put("items", itemMapper.selectCount(new QueryWrapper<LfItem>()));
        stats.put("pendingReview", itemMapper.selectCount(new QueryWrapper<LfItem>().eq("status", "PENDING_REVIEW")));
        stats.put("published", itemMapper.selectCount(new QueryWrapper<LfItem>().eq("status", "PUBLISHED")));
        stats.put("handoverPending", itemMapper.selectCount(new QueryWrapper<LfItem>().eq("status", "HANDOVER_PENDING")));
        stats.put("completed", itemMapper.selectCount(new QueryWrapper<LfItem>().eq("status", "COMPLETED")));
        stats.put("pendingClaims", claimMapper.selectCount(new QueryWrapper<LfClaimApplication>().eq("status", "PENDING")));
        return stats;
    }

    private void requireAdmin() {
        PermissionGuard.requireRole(LoginContext.get(), "ADMIN");
    }

    private Page<SysUser> userPage(Map<String, String> query) {
        return Page.of(pageNum(query), pageSize(query));
    }

    private Page<LfCategory> categoryPage(Map<String, String> query) {
        return Page.of(pageNum(query), pageSize(query));
    }

    private Page<LfLocation> locationPage(Map<String, String> query) {
        return Page.of(pageNum(query), pageSize(query));
    }

    private Page<SysNotice> noticePage(Map<String, String> query) {
        return Page.of(pageNum(query), pageSize(query));
    }

    private Page<SysOperationLog> logPage(Map<String, String> query) {
        return Page.of(pageNum(query), pageSize(query));
    }

    private long pageNum(Map<String, String> query) {
        return query.get("pageNum") == null ? 1 : Long.parseLong(query.get("pageNum"));
    }

    private long pageSize(Map<String, String> query) {
        return query.get("pageSize") == null ? 10 : Long.parseLong(query.get("pageSize"));
    }

    private String blankDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private Integer booleanFlag(Map<String, Object> body, String key) {
        Object value = body.get(key);
        if (value == null) {
            return 0;
        }
        if (value instanceof Boolean bool) {
            return bool ? 1 : 0;
        }
        String text = String.valueOf(value);
        return "1".equals(text) || "true".equalsIgnoreCase(text) ? 1 : 0;
    }
}
