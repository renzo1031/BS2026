package com.campus.lostfound.lostfound.item.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.lostfound.common.BizException;
import com.campus.lostfound.common.IdGenerator;
import com.campus.lostfound.common.MapReader;
import com.campus.lostfound.common.PageResult;
import com.campus.lostfound.lostfound.claim.entity.LfClaimApplication;
import com.campus.lostfound.lostfound.claim.mapper.LfClaimApplicationMapper;
import com.campus.lostfound.lostfound.clue.entity.LfClueFeedback;
import com.campus.lostfound.lostfound.clue.mapper.LfClueFeedbackMapper;
import com.campus.lostfound.lostfound.custody.entity.LfCustodyHandover;
import com.campus.lostfound.lostfound.custody.mapper.LfCustodyHandoverMapper;
import com.campus.lostfound.lostfound.item.ItemStateMachine;
import com.campus.lostfound.lostfound.item.entity.LfItem;
import com.campus.lostfound.lostfound.item.entity.LfItemImage;
import com.campus.lostfound.lostfound.item.mapper.LfItemImageMapper;
import com.campus.lostfound.lostfound.item.mapper.LfItemMapper;
import com.campus.lostfound.security.CurrentUser;
import com.campus.lostfound.security.LoginContext;
import com.campus.lostfound.security.PermissionGuard;
import com.campus.lostfound.system.log.entity.SysOperationLog;
import com.campus.lostfound.system.log.mapper.SysOperationLogMapper;
import com.campus.lostfound.system.log.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ItemService {
    private final LfItemMapper itemMapper;
    private final LfItemImageMapper itemImageMapper;
    private final LfClaimApplicationMapper claimMapper;
    private final LfClueFeedbackMapper clueMapper;
    private final LfCustodyHandoverMapper handoverMapper;
    private final SysOperationLogMapper logMapper;
    private final OperationLogService logService;

    public PageResult<LfItem> publicPage(Map<String, String> query) {
        QueryWrapper<LfItem> wrapper = new QueryWrapper<>();
        wrapper.eq("status", "PUBLISHED");
        wrapper.eq("deleted", 0);
        likeIfPresent(wrapper, "title", query.get("keyword"));
        eqIfPresent(wrapper, "type", query.get("type"));
        eqIfPresent(wrapper, "category_id", query.get("categoryId"));
        eqIfPresent(wrapper, "location_id", query.get("locationId"));
        wrapper.orderByDesc("last_operation_time", "created_at");
        return PageResult.from(itemMapper.selectPage(page(query), wrapper));
    }

    public Map<String, Object> publicDetail(Long id) {
        LfItem item = getItem(id);
        if (!"PUBLISHED".equals(item.getStatus()) && LoginContext.userIdOrNull() == null) {
            throw new BizException("物品不存在或未公开");
        }
        return detailMap(item);
    }

    @Transactional
    public LfItem create(Map<String, Object> body) {
        CurrentUser user = LoginContext.get();
        LfItem item = new LfItem();
        item.setId(IdGenerator.nextId());
        item.setItemNo(generateItemNo(MapReader.requiredStr(body, "type", "物品类型")));
        fillItemFields(item, body);
        item.setStatus("DRAFT");
        item.setPublisherId(user.id());
        touch(item, user.id(), "用户创建物品草稿");
        item.setCreatedBy(user.id());
        item.setUpdatedBy(user.id());
        itemMapper.insert(item);
        logService.record("ITEM", item.getId(), "CREATE", null, "DRAFT", "创建物品草稿");
        return item;
    }

    @Transactional
    public LfItem update(Long id, Map<String, Object> body) {
        CurrentUser user = LoginContext.get();
        LfItem item = getItem(id);
        PermissionGuard.requireOwnerOrAdmin(user, item.getPublisherId());
        if (!user.hasRole("ADMIN") && !List.of("DRAFT", "REJECTED").contains(item.getStatus())) {
            throw new BizException("当前状态不可编辑");
        }
        fillItemFields(item, body);
        touch(item, user.id(), "修改物品信息");
        item.setUpdatedBy(user.id());
        itemMapper.updateById(item);
        logService.record("ITEM", id, "UPDATE", item.getStatus(), item.getStatus(), "修改物品信息");
        return item;
    }

    @Transactional
    public void submit(Long id) {
        CurrentUser user = LoginContext.get();
        LfItem item = getItem(id);
        PermissionGuard.requireOwnerOrAdmin(user, item.getPublisherId());
        String before = item.getStatus();
        ItemStateMachine.assertTransition(before, "PENDING_REVIEW");
        item.setStatus("PENDING_REVIEW");
        touch(item, user.id(), "提交审核");
        itemMapper.updateById(item);
        logService.record("ITEM", id, "SUBMIT_REVIEW", before, "PENDING_REVIEW", "提交审核");
    }

    public PageResult<Map<String, Object>> adminPage(Map<String, String> query) {
        CurrentUser user = LoginContext.get();
        PermissionGuard.requireAnyRole(user, "ADMIN", "STAFF");
        String status = user.hasRole("ADMIN") ? query.get("status") : "HANDOVER_PENDING";
        Page<Map<String, Object>> page = Page.of(pageNum(query), pageSize(query));
        return PageResult.from(itemMapper.selectAdminItemPage(
                page,
                query.get("keyword"),
                query.get("type"),
                status,
                query.get("categoryId"),
                query.get("locationId"),
                query.get("publisherKeyword"),
                query.get("claimantKeyword"),
                query.get("reviewerKeyword")
        ));
    }

    public Map<String, Object> adminDetail(Long id) {
        PermissionGuard.requireRole(LoginContext.get(), "ADMIN");
        return detailMap(getItem(id));
    }

    @Transactional
    public void review(Long id, Map<String, Object> body) {
        CurrentUser admin = LoginContext.get();
        PermissionGuard.requireRole(admin, "ADMIN");
        LfItem item = getItem(id);
        if (!"PENDING_REVIEW".equals(item.getStatus())) {
            throw new BizException("只有待审核物品可以审核");
        }
        String result = MapReader.requiredStr(body, "result", "审核结果");
        String reason = MapReader.requiredStr(body, "reason", "审核原因");
        String before = item.getStatus();
        String after = "APPROVED".equals(result) ? "PUBLISHED" : "REJECTED";
        ItemStateMachine.assertTransition(before, after);
        item.setStatus(after);
        item.setReviewerId(admin.id());
        item.setReviewTime(LocalDateTime.now());
        item.setReviewResult(result);
        item.setReviewReason(reason);
        touch(item, admin.id(), "管理员审核：" + result);
        itemMapper.updateById(item);
        logService.record("ITEM", id, "APPROVED".equals(result) ? "REVIEW_APPROVE" : "REVIEW_REJECT", before, after, reason);
    }

    @Transactional
    public void offline(Long id, Map<String, Object> body) {
        CurrentUser admin = LoginContext.get();
        PermissionGuard.requireRole(admin, "ADMIN");
        LfItem item = getItem(id);
        String reason = MapReader.requiredStr(body, "reason", "下架原因");
        String before = item.getStatus();
        ItemStateMachine.assertTransition(before, "OFFLINE");
        item.setStatus("OFFLINE");
        item.setOfflineReason(reason);
        touch(item, admin.id(), "管理员下架");
        itemMapper.updateById(item);
        logService.record("ITEM", id, "OFFLINE", before, "OFFLINE", reason);
    }

    @Transactional
    public void archive(Long id, Map<String, Object> body) {
        CurrentUser admin = LoginContext.get();
        PermissionGuard.requireRole(admin, "ADMIN");
        LfItem item = getItem(id);
        String reason = MapReader.requiredStr(body, "reason", "归档原因");
        String before = item.getStatus();
        ItemStateMachine.assertTransition(before, "ARCHIVED");
        item.setStatus("ARCHIVED");
        touch(item, admin.id(), "管理员归档");
        itemMapper.updateById(item);
        logService.record("ITEM", id, "ARCHIVE", before, "ARCHIVED", reason);
    }

    public PageResult<LfItem> myItems(Map<String, String> query) {
        CurrentUser user = LoginContext.get();
        QueryWrapper<LfItem> wrapper = new QueryWrapper<>();
        wrapper.eq("publisher_id", user.id()).eq("deleted", 0).orderByDesc("updated_at");
        return PageResult.from(itemMapper.selectPage(page(query), wrapper));
    }

    private Map<String, Object> detailMap(LfItem item) {
        Map<String, Object> detail = new HashMap<>();
        detail.put("item", item);
        detail.put("images", itemImageMapper.selectList(new LambdaQueryWrapper<LfItemImage>().eq(LfItemImage::getItemId, item.getId())));
        detail.put("claims", claimMapper.selectList(new LambdaQueryWrapper<LfClaimApplication>().eq(LfClaimApplication::getItemId, item.getId())));
        detail.put("clues", clueMapper.selectList(new LambdaQueryWrapper<LfClueFeedback>().eq(LfClueFeedback::getItemId, item.getId())));
        detail.put("handovers", handoverMapper.selectList(new LambdaQueryWrapper<LfCustodyHandover>().eq(LfCustodyHandover::getItemId, item.getId())));
        detail.put("timeline", logMapper.selectList(new LambdaQueryWrapper<SysOperationLog>()
                .eq(SysOperationLog::getTargetType, "ITEM")
                .eq(SysOperationLog::getTargetId, item.getId())
                .orderByAsc(SysOperationLog::getCreatedAt)));
        return detail;
    }

    private LfItem getItem(Long id) {
        LfItem item = itemMapper.selectById(id);
        if (item == null) {
            throw new BizException("物品不存在");
        }
        return item;
    }

    private void fillItemFields(LfItem item, Map<String, Object> body) {
        item.setType(MapReader.requiredStr(body, "type", "物品类型"));
        item.setTitle(MapReader.requiredStr(body, "title", "标题"));
        item.setCategoryId(MapReader.requiredLong(body, "categoryId", "分类"));
        item.setLocationId(MapReader.requiredLong(body, "locationId", "地点"));
        LocalDateTime eventTime = MapReader.dateTime(body, "eventTime");
        item.setEventTime(eventTime == null ? LocalDateTime.now() : eventTime);
        item.setDescription(MapReader.requiredStr(body, "description", "描述"));
        item.setContactName(MapReader.requiredStr(body, "contactName", "联系人"));
        item.setContactPhone(MapReader.requiredStr(body, "contactPhone", "联系电话"));
    }

    private void touch(LfItem item, Long operatorId, String summary) {
        item.setLastOperatorId(operatorId);
        item.setLastOperationSummary(summary);
        item.setLastOperationTime(LocalDateTime.now());
        item.setUpdatedBy(operatorId);
    }

    private String generateItemNo(String type) {
        String prefix = "LOST".equals(type) ? "LOST" : "FOUND";
        return prefix + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + (int) (Math.random() * 900 + 100);
    }

    private Page<LfItem> page(Map<String, String> query) {
        return Page.of(pageNum(query), pageSize(query));
    }

    private long pageNum(Map<String, String> query) {
        return query.get("pageNum") == null ? 1 : Long.parseLong(query.get("pageNum"));
    }

    private long pageSize(Map<String, String> query) {
        return query.get("pageSize") == null ? 10 : Long.parseLong(query.get("pageSize"));
    }

    private void likeIfPresent(QueryWrapper<LfItem> wrapper, String column, String value) {
        if (value != null && !value.isBlank()) {
            wrapper.like(column, value);
        }
    }

    private void eqIfPresent(QueryWrapper<LfItem> wrapper, String column, String value) {
        if (value != null && !value.isBlank()) {
            wrapper.eq(column, value);
        }
    }
}
