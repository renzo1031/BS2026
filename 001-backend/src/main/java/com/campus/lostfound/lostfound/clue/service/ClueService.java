package com.campus.lostfound.lostfound.clue.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.lostfound.common.BizException;
import com.campus.lostfound.common.IdGenerator;
import com.campus.lostfound.common.MapReader;
import com.campus.lostfound.common.PageResult;
import com.campus.lostfound.lostfound.clue.entity.LfClueFeedback;
import com.campus.lostfound.lostfound.clue.mapper.LfClueFeedbackMapper;
import com.campus.lostfound.lostfound.item.entity.LfItem;
import com.campus.lostfound.lostfound.item.mapper.LfItemMapper;
import com.campus.lostfound.security.CurrentUser;
import com.campus.lostfound.security.LoginContext;
import com.campus.lostfound.security.PermissionGuard;
import com.campus.lostfound.system.log.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ClueService {
    private final LfClueFeedbackMapper clueMapper;
    private final LfItemMapper itemMapper;
    private final OperationLogService logService;

    @Transactional
    public LfClueFeedback create(Long itemId, Map<String, Object> body) {
        CurrentUser user = LoginContext.get();
        LfItem item = getItem(itemId);
        if (!"PUBLISHED".equals(item.getStatus())) {
            throw new BizException("当前物品不可提交线索");
        }
        LfClueFeedback clue = new LfClueFeedback();
        clue.setId(IdGenerator.nextId());
        clue.setItemId(itemId);
        clue.setSubmitterId(user.id());
        clue.setClueContent(MapReader.requiredStr(body, "clueContent", "线索内容"));
        clue.setClueImageUrl(MapReader.str(body, "clueImageUrl"));
        clue.setContactPhone(MapReader.str(body, "contactPhone"));
        clue.setStatus("PENDING_CONFIRM");
        clue.setCreatedBy(user.id());
        clue.setUpdatedBy(user.id());
        clueMapper.insert(clue);
        logService.record("CLUE", clue.getId(), "CLUE_CREATE", null, "PENDING_CONFIRM", "提交线索");
        return clue;
    }

    public PageResult<LfClueFeedback> mine(Map<String, String> query) {
        CurrentUser user = LoginContext.get();
        QueryWrapper<LfClueFeedback> wrapper = new QueryWrapper<>();
        wrapper.eq("submitter_id", user.id()).eq("deleted", 0).orderByDesc("created_at");
        return PageResult.from(clueMapper.selectPage(page(query), wrapper));
    }

    public PageResult<Map<String, Object>> adminPage(Map<String, String> query) {
        PermissionGuard.requireRole(LoginContext.get(), "ADMIN");
        return PageResult.from(clueMapper.selectAdminPage(Page.of(pageNum(query), pageSize(query)), query.get("status")));
    }

    @Transactional
    public void confirm(Long id, Map<String, Object> body) {
        CurrentUser user = LoginContext.get();
        LfClueFeedback clue = clueMapper.selectById(id);
        if (clue == null) {
            throw new BizException("线索不存在");
        }
        LfItem item = getItem(clue.getItemId());
        if (!user.hasRole("ADMIN") && !item.getPublisherId().equals(user.id())) {
            throw new SecurityException("只有发布人或管理员可以确认线索");
        }
        String result = MapReader.requiredStr(body, "result", "确认结果");
        String after = "VALID".equals(result) ? "VALID" : "INVALID";
        String before = clue.getStatus();
        clue.setStatus(after);
        clue.setConfirmerId(user.id());
        clue.setConfirmTime(LocalDateTime.now());
        clue.setConfirmReason(MapReader.str(body, "reason"));
        clue.setUpdatedBy(user.id());
        clueMapper.updateById(clue);
        logService.record("CLUE", id, "CLUE_CONFIRM", before, after, clue.getConfirmReason());
    }

    private LfItem getItem(Long id) {
        LfItem item = itemMapper.selectById(id);
        if (item == null) {
            throw new BizException("物品不存在");
        }
        return item;
    }

    private Page<LfClueFeedback> page(Map<String, String> query) {
        return Page.of(pageNum(query), pageSize(query));
    }

    private long pageNum(Map<String, String> query) {
        return query.get("pageNum") == null ? 1 : Long.parseLong(query.get("pageNum"));
    }

    private long pageSize(Map<String, String> query) {
        return query.get("pageSize") == null ? 10 : Long.parseLong(query.get("pageSize"));
    }
}
