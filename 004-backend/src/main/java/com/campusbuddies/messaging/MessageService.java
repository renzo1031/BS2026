package com.campusbuddies.messaging;

import com.campusbuddies.activity.BuddyActivity;
import com.campusbuddies.activity.BuddyActivityMapper;
import com.campusbuddies.activity.Conversation;
import com.campusbuddies.activity.ConversationMapper;
import com.campusbuddies.common.BusinessException;
import com.campusbuddies.common.ErrorCode;
import com.campusbuddies.file.FileService;
import com.campusbuddies.security.SecuritySupport;
import com.campusbuddies.user.SysUser;
import com.campusbuddies.user.SysUserMapper;
import com.campusbuddies.user.UserStatus;
import com.campusbuddies.user.VerificationStatus;
import java.time.Instant;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class MessageService {
    public record MessageView(long id, long conversationId, long senderId, String senderNickname,
                              String clientMessageId, MessageType messageType, String content,
                              Long fileId, Instant createdAt, boolean duplicate) {}
    public record ConversationView(long id, long activityId, String activityTitle,
                                   String status, Long lastMessageId, Instant updatedAt) {}

    private final MessageMapper messages;
    private final ConversationMapper conversations;
    private final BuddyActivityMapper activities;
    private final SysUserMapper users;
    private final FileService files;
    private final ContentSafetyGateway contentSafety;

    public MessageService(MessageMapper messages, ConversationMapper conversations,
                          BuddyActivityMapper activities, SysUserMapper users,
                          FileService files, ContentSafetyGateway contentSafety) {
        this.messages = messages;
        this.conversations = conversations;
        this.activities = activities;
        this.users = users;
        this.files = files;
        this.contentSafety = contentSafety;
    }

    public List<ConversationView> conversations() {
        long userId = SecuritySupport.current().userId();
        return conversations.findForUser(userId).stream().map(conversation -> {
            BuddyActivity activity = activities.selectById(conversation.getActivityId());
            return new ConversationView(conversation.getId(), conversation.getActivityId(),
                    activity == null ? "活动已失效" : activity.getTitle(), conversation.getStatus(),
                    conversation.getLastMessageId(), conversation.getUpdatedAt());
        }).toList();
    }

    public List<MessageView> history(long conversationId, long afterId, int limit) {
        long userId = SecuritySupport.current().userId();
        requireMember(conversationId, userId);
        if (afterId < 0 || limit < 1 || limit > 100) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "游标或每页数量不合法");
        }
        return messages.findAfter(conversationId, afterId, limit).stream().map(message -> view(message, false)).toList();
    }

    @Transactional
    public MessageView send(long userId, long conversationId, String clientMessageId,
                            MessageType messageType, String content, Long fileId) {
        SysUser sender = users.selectById(userId);
        if (sender == null || sender.getStatus() != UserStatus.ACTIVE
                || sender.getVerificationStatus() != VerificationStatus.APPROVED) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "当前账号不能发送消息");
        }
        Conversation conversation = requireMember(conversationId, userId);
        if (!"OPEN".equals(conversation.getStatus())) {
            throw new BusinessException(ErrorCode.INVALID_STATE_TRANSITION, "当前会话不可发送消息");
        }
        String clientId = validateClientMessageId(clientMessageId);
        Message existing = messages.findByClientId(conversationId, userId, clientId);
        if (existing != null) return view(existing, true);
        if (messageType == null || messageType == MessageType.SYSTEM) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "消息类型不合法");
        }
        String cleanContent = null;
        Long safeFileId = null;
        if (messageType == MessageType.TEXT) {
            cleanContent = validateContent(content);
            if (fileId != null) throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "文本消息不能携带图片");
            contentSafety.validateText(cleanContent);
        } else {
            if (fileId == null) throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "图片消息缺少 fileId");
            if (StringUtils.hasText(content)) throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "图片消息不支持附加文本");
            BuddyActivity activity = activities.selectById(conversation.getActivityId());
            if (activity == null) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
            files.requireOwnedApprovedChatImage(fileId, userId, activity.getCampusId());
            safeFileId = fileId;
        }
        Message message = new Message();
        message.setConversationId(conversationId);
        message.setSenderId(userId);
        message.setClientMessageId(clientId);
        message.setMessageType(messageType);
        message.setContent(cleanContent);
        message.setFileId(safeFileId);
        message.setStatus(MessageStatus.VISIBLE);
        try {
            messages.insert(message);
        } catch (DuplicateKeyException ex) {
            Message duplicate = messages.findByClientId(conversationId, userId, clientId);
            if (duplicate != null) return view(duplicate, true);
            throw ex;
        }
        if (safeFileId != null) files.bindChatImage(safeFileId, userId, message.getId());
        conversations.updateLastMessage(conversationId, message.getId());
        return view(message, false);
    }

    @Transactional
    public MessageView sendCurrent(long conversationId, String clientMessageId,
                                   MessageType type, String content, Long fileId) {
        return send(SecuritySupport.current().userId(), conversationId, clientMessageId, type, content, fileId);
    }

    public List<Long> participantIds(long conversationId) {
        return conversations.findActiveUserIds(conversationId);
    }

    private Conversation requireMember(long conversationId, long userId) {
        Conversation conversation = conversations.selectById(conversationId);
        if (conversation == null || !conversations.findActiveUserIds(conversationId).contains(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该会话");
        }
        return conversation;
    }

    private MessageView view(Message message, boolean duplicate) {
        SysUser sender = users.selectById(message.getSenderId());
        return new MessageView(message.getId(), message.getConversationId(), message.getSenderId(),
                sender == null ? "未知用户" : sender.getNickname(), message.getClientMessageId(),
                message.getMessageType(), message.getContent(), message.getFileId(), message.getCreatedAt(), duplicate);
    }

    private String validateClientMessageId(String value) {
        if (!StringUtils.hasText(value)) throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "clientMessageId 不能为空");
        String clean = value.trim();
        if (!clean.matches("[A-Za-z0-9_-]{1,64}")) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "clientMessageId 格式不正确");
        }
        return clean;
    }

    private String validateContent(String value) {
        if (!StringUtils.hasText(value)) throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "消息内容不能为空");
        String clean = value.trim();
        if (clean.codePointCount(0, clean.length()) > 1000) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "消息内容不能超过 1000 字");
        }
        return clean;
    }
}
