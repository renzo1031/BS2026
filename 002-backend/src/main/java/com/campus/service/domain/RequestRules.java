package com.campus.service.domain;

import com.campus.service.common.BusinessException;
import com.campus.service.entity.ServiceRequest;
import com.campus.service.entity.Venue;

import java.time.LocalDateTime;

public final class RequestRules {
    private RequestRules() {
    }

    public static void validateCommon(ServiceRequest request) {
        requireText(request.getTitle(), "申请标题", 120);
        requireText(request.getContent(), "申请内容", 10000);
    }

    public static void validateRepair(ServiceRequest request) {
        requireText(request.getLocation(), "故障地点", 120);
        requireText(request.getRepairCategory(), "故障类型", 50);
        requireText(request.getUrgency(), "紧急程度", 20);
    }

    public static void validateCertificate(ServiceRequest request) {
        requireText(request.getCertificateType(), "证明类型", 50);
        requireText(request.getPurpose(), "证明用途", 255);
        requireText(request.getLanguage(), "证明语言", 20);
        requireText(request.getDeliveryMethod(), "领取方式", 30);
        if (request.getCopies() == null || request.getCopies() < 1) {
            throw new BusinessException("证明份数必须大于0");
        }
    }

    public static void validateVenue(ServiceRequest request, Venue venue, LocalDateTime now) {
        requireText(request.getEventName(), "活动名称", 120);
        requireText(request.getContactName(), "联系人", 50);
        requireText(request.getContactPhone(), "联系电话", 30);
        if (request.getAppointmentStart() == null || request.getAppointmentEnd() == null) {
            throw new BusinessException("场地申请必须填写开始和结束时间");
        }
        if (!request.getAppointmentStart().isAfter(now)) {
            throw new BusinessException("场地开始时间必须晚于当前时间");
        }
        if (!request.getAppointmentEnd().isAfter(request.getAppointmentStart())) {
            throw new BusinessException("场地结束时间必须晚于开始时间");
        }
        if (request.getAttendeeCount() == null || request.getAttendeeCount() < 1) {
            throw new BusinessException("参加人数必须大于0");
        }
        if (venue == null) {
            throw new BusinessException(404, "场地不存在");
        }
        if (!"AVAILABLE".equals(venue.getStatus())) {
            throw new BusinessException(409, "场地当前不可用");
        }
        if (venue.getCapacity() == null || request.getAttendeeCount() > venue.getCapacity()) {
            throw new BusinessException("参加人数不能超过场地容量");
        }
    }

    public static void validateApproval(Boolean approved, String comment) {
        if (approved == null) {
            throw new BusinessException("审核结论不能为空");
        }
        requireText(comment, approved ? "审核意见" : "驳回理由", 500);
    }

    public static void validateFinish(String result) {
        requireText(result, "办结结果", 500);
    }

    public static void validateFeedback(Integer score, String content) {
        if (score == null || score < 1 || score > 5) {
            throw new BusinessException("评分必须在1到5之间");
        }
        if (content != null && content.length() > 500) {
            throw new BusinessException("评价内容不能超过500个字符");
        }
    }

    public static void validatePage(long page, long size) {
        if (page < 1 || size < 1 || size > 100) {
            throw new BusinessException("分页参数必须满足 page >= 1 且 1 <= size <= 100");
        }
    }

    public static void requireTransition(String current, RequestStatus target) {
        RequestStatus from;
        try {
            from = RequestStatus.valueOf(current);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new BusinessException("未知申请状态");
        }
        if (!from.canTransitionTo(target)) {
            throw new BusinessException("不允许从 " + from + " 变更为 " + target);
        }
    }

    public static String clean(String value) {
        return value == null ? null : value.trim();
    }

    private static void requireText(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(field + "不能为空");
        }
        if (value.length() > maxLength) {
            throw new BusinessException(field + "不能超过" + maxLength + "个字符");
        }
    }
}
