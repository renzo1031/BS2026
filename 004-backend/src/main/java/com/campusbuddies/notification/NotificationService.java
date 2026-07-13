package com.campusbuddies.notification;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campusbuddies.common.BusinessException;
import com.campusbuddies.common.ErrorCode;
import com.campusbuddies.common.PageResult;
import com.campusbuddies.security.SecuritySupport;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {
    public record View(long id, String type, String title, String content, String targetType,
                       Long targetId, boolean read, java.time.Instant createdAt) {}

    private final NotificationMapper notifications;

    public NotificationService(NotificationMapper notifications) { this.notifications = notifications; }

    public void create(long userId, String type, String title, String content, String targetType, Long targetId) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(limit(type, 32));
        notification.setTitle(limit(title, 80));
        notification.setContent(limit(content, 500));
        notification.setTargetType(targetType == null ? null : limit(targetType, 32));
        notification.setTargetId(targetId);
        notification.setIsRead(false);
        notification.setDeliveryStatus("IN_APP_ONLY");
        notification.setRetryCount(0);
        notifications.insert(notification);
    }

    public PageResult<View> mine(int page, int size, Boolean unreadOnly) {
        long userId = SecuritySupport.current().userId();
        int safeSize = PageResult.safeSize(size);
        if (page < 1) throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "page 必须大于等于 1");
        LambdaQueryWrapper<Notification> query = new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .orderByDesc(Notification::getCreatedAt);
        if (Boolean.TRUE.equals(unreadOnly)) query.eq(Notification::getIsRead, false);
        IPage<Notification> result = notifications.selectPage(Page.of(page, safeSize), query);
        return new PageResult<>(result.getRecords().stream().map(this::view).toList(),
                result.getTotal(), page, safeSize);
    }

    @Transactional
    public void markRead(long id) {
        long userId = SecuritySupport.current().userId();
        Notification notification = notifications.selectById(id);
        if (notification == null || !notification.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        notifications.markRead(id, userId);
    }

    @Transactional
    public void markAllRead() {
        notifications.markAllRead(SecuritySupport.current().userId());
    }

    private View view(Notification item) {
        return new View(item.getId(), item.getType(), item.getTitle(), item.getContent(), item.getTargetType(),
                item.getTargetId(), Boolean.TRUE.equals(item.getIsRead()), item.getCreatedAt());
    }

    private String limit(String value, int max) {
        if (value == null) return "";
        return value.length() <= max ? value : value.substring(0, max);
    }
}
