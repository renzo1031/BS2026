package com.campus.lostfound.lostfound.claim.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.lostfound.common.BizException;
import com.campus.lostfound.common.IdGenerator;
import com.campus.lostfound.common.MapReader;
import com.campus.lostfound.common.PageResult;
import com.campus.lostfound.lostfound.claim.entity.LfClaimApplication;
import com.campus.lostfound.lostfound.claim.mapper.LfClaimApplicationMapper;
import com.campus.lostfound.lostfound.custody.entity.LfCustodyHandover;
import com.campus.lostfound.lostfound.custody.mapper.LfCustodyHandoverMapper;
import com.campus.lostfound.lostfound.item.ItemStateMachine;
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
public class ClaimService {
    private final LfClaimApplicationMapper claimMapper;
    private final LfItemMapper itemMapper;
    private final LfCustodyHandoverMapper handoverMapper;
    private final OperationLogService logService;

    @Transactional
    public LfClaimApplication create(Long itemId, Map<String, Object> body) {
        CurrentUser user = LoginContext.get();
        LfItem item = getItem(itemId);
        if (!"PUBLISHED".equals(item.getStatus())) {
            throw new BizException("当前物品不可认领");
        }
        if (item.getPublisherId().equals(user.id())) {
            throw new BizException("不能认领自己发布的招领物品");
        }
        String before = item.getStatus();
        ItemStateMachine.assertTransition(before, "CLAIM_REVIEWING");

        LfClaimApplication claim = new LfClaimApplication();
        claim.setId(IdGenerator.nextId());
        claim.setItemId(itemId);
        claim.setApplicantId(user.id());
        claim.setApplicantName(MapReader.requiredStr(body, "applicantName", "申请人姓名"));
        claim.setApplicantPhone(MapReader.requiredStr(body, "applicantPhone", "申请人电话"));
        claim.setProofText(MapReader.requiredStr(body, "proofText", "证明材料"));
        claim.setProofImageUrl(MapReader.str(body, "proofImageUrl"));
        claim.setStatus("PENDING");
        claim.setCreatedBy(user.id());
        claim.setUpdatedBy(user.id());
        claimMapper.insert(claim);

        item.setStatus("CLAIM_REVIEWING");
        item.setCurrentClaimantId(user.id());
        touchItem(item, user.id(), "用户提交认领申请");
        itemMapper.updateById(item);

        logService.record("CLAIM", claim.getId(), "CLAIM_CREATE", null, "PENDING", "提交认领申请");
        logService.record("ITEM", itemId, "CLAIM_CREATE", before, "CLAIM_REVIEWING", "用户提交认领申请");
        return claim;
    }

    public PageResult<LfClaimApplication> mine(Map<String, String> query) {
        CurrentUser user = LoginContext.get();
        QueryWrapper<LfClaimApplication> wrapper = new QueryWrapper<>();
        wrapper.eq("applicant_id", user.id()).eq("deleted", 0).orderByDesc("created_at");
        return PageResult.from(claimMapper.selectPage(page(query), wrapper));
    }

    public PageResult<LfClaimApplication> staffPage(Map<String, String> query) {
        PermissionGuard.requireAnyRole(LoginContext.get(), "STAFF", "ADMIN");
        QueryWrapper<LfClaimApplication> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted", 0);
        if (query.get("status") != null && !query.get("status").isBlank()) {
            wrapper.eq("status", query.get("status"));
        }
        wrapper.orderByDesc("created_at");
        return PageResult.from(claimMapper.selectPage(page(query), wrapper));
    }

    @Transactional
    public void updateCustody(Long itemId, Map<String, Object> body) {
        CurrentUser staff = LoginContext.get();
        PermissionGuard.requireAnyRole(staff, "STAFF", "ADMIN");
        LfItem item = getItem(itemId);
        String location = MapReader.requiredStr(body, "custodyLocation", "保管位置");
        item.setCustodianId(staff.id());
        item.setCustodyLocation(location);
        touchItem(item, staff.id(), "更新保管位置");
        itemMapper.updateById(item);
        logService.record("ITEM", itemId, "CUSTODY_UPDATE", item.getStatus(), item.getStatus(), "更新保管位置：" + location);
    }

    @Transactional
    public void approve(Long claimId, Map<String, Object> body) {
        CurrentUser staff = LoginContext.get();
        PermissionGuard.requireAnyRole(staff, "STAFF", "ADMIN");
        LfClaimApplication claim = getClaim(claimId);
        if (!"PENDING".equals(claim.getStatus())) {
            throw new BizException("只有待审核认领可以通过");
        }
        LfItem item = getItem(claim.getItemId());
        String itemBefore = item.getStatus();
        ItemStateMachine.assertTransition(itemBefore, "HANDOVER_PENDING");

        String reason = MapReader.requiredStr(body, "reason", "核验意见");
        claim.setStatus("APPROVED");
        claim.setReviewerId(staff.id());
        claim.setReviewTime(LocalDateTime.now());
        claim.setReviewReason(reason);
        claim.setUpdatedBy(staff.id());
        claimMapper.updateById(claim);

        item.setStatus("HANDOVER_PENDING");
        item.setCustodianId(staff.id());
        item.setCustodyLocation(MapReader.str(body, "custodyLocation"));
        touchItem(item, staff.id(), "保管员核验通过，待交接");
        itemMapper.updateById(item);

        LfCustodyHandover handover = new LfCustodyHandover();
        handover.setId(IdGenerator.nextId());
        handover.setItemId(item.getId());
        handover.setClaimId(claim.getId());
        handover.setCustodianId(staff.id());
        handover.setCustodyLocation(item.getCustodyLocation() == null ? "失物招领中心" : item.getCustodyLocation());
        handover.setReceiverId(claim.getApplicantId());
        handover.setReceiverName(claim.getApplicantName());
        handover.setReceiverPhone(claim.getApplicantPhone());
        handover.setHandlerId(staff.id());
        handover.setStatus("HANDOVER_PENDING");
        handover.setRemark(reason);
        handover.setCreatedBy(staff.id());
        handover.setUpdatedBy(staff.id());
        handoverMapper.insert(handover);

        logService.record("CLAIM", claimId, "CLAIM_APPROVE", "PENDING", "APPROVED", reason);
        logService.record("ITEM", item.getId(), "CLAIM_APPROVE", itemBefore, "HANDOVER_PENDING", reason);
    }

    @Transactional
    public void reject(Long claimId, Map<String, Object> body) {
        CurrentUser staff = LoginContext.get();
        PermissionGuard.requireAnyRole(staff, "STAFF", "ADMIN");
        LfClaimApplication claim = getClaim(claimId);
        if (!"PENDING".equals(claim.getStatus())) {
            throw new BizException("只有待审核认领可以驳回");
        }
        LfItem item = getItem(claim.getItemId());
        String reason = MapReader.requiredStr(body, "reason", "驳回原因");
        claim.setStatus("REJECTED");
        claim.setReviewerId(staff.id());
        claim.setReviewTime(LocalDateTime.now());
        claim.setReviewReason(reason);
        claim.setUpdatedBy(staff.id());
        claimMapper.updateById(claim);

        String before = item.getStatus();
        item.setStatus("PUBLISHED");
        item.setCurrentClaimantId(null);
        touchItem(item, staff.id(), "认领申请驳回，重新上架");
        itemMapper.updateById(item);
        logService.record("CLAIM", claimId, "CLAIM_REJECT", "PENDING", "REJECTED", reason);
        logService.record("ITEM", item.getId(), "CLAIM_REJECT", before, "PUBLISHED", reason);
    }

    @Transactional
    public void handover(Long itemId, Map<String, Object> body) {
        CurrentUser staff = LoginContext.get();
        PermissionGuard.requireAnyRole(staff, "STAFF", "ADMIN");
        LfItem item = getItem(itemId);
        if (!"HANDOVER_PENDING".equals(item.getStatus())) {
            throw new BizException("当前物品不在待交接状态");
        }
        String before = item.getStatus();
        ItemStateMachine.assertTransition(before, "COMPLETED");
        LfCustodyHandover handover = handoverMapper.selectOne(new LambdaQueryWrapper<LfCustodyHandover>().eq(LfCustodyHandover::getItemId, itemId).orderByDesc(LfCustodyHandover::getCreatedAt).last("LIMIT 1"));
        if (handover != null) {
            handover.setHandoverLocation(MapReader.requiredStr(body, "handoverLocation", "交接地点"));
            handover.setHandoverTime(LocalDateTime.now());
            handover.setHandlerId(staff.id());
            handover.setStatus("COMPLETED");
            handover.setRemark(MapReader.str(body, "remark"));
            handover.setUpdatedBy(staff.id());
            handoverMapper.updateById(handover);
        }
        item.setStatus("COMPLETED");
        item.setCompletedTime(LocalDateTime.now());
        touchItem(item, staff.id(), "线下交接完成");
        itemMapper.updateById(item);
        logService.record("ITEM", itemId, "HANDOVER_COMPLETE", before, "COMPLETED", "线下交接完成");
    }

    private LfClaimApplication getClaim(Long id) {
        LfClaimApplication claim = claimMapper.selectById(id);
        if (claim == null) {
            throw new BizException("认领申请不存在");
        }
        return claim;
    }

    private LfItem getItem(Long id) {
        LfItem item = itemMapper.selectById(id);
        if (item == null) {
            throw new BizException("物品不存在");
        }
        return item;
    }

    private void touchItem(LfItem item, Long operatorId, String summary) {
        item.setLastOperatorId(operatorId);
        item.setLastOperationSummary(summary);
        item.setLastOperationTime(LocalDateTime.now());
        item.setUpdatedBy(operatorId);
    }

    private Page<LfClaimApplication> page(Map<String, String> query) {
        long pageNum = query.get("pageNum") == null ? 1 : Long.parseLong(query.get("pageNum"));
        long pageSize = query.get("pageSize") == null ? 10 : Long.parseLong(query.get("pageSize"));
        return Page.of(pageNum, pageSize);
    }
}
