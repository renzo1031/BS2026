package com.campusbuddies.messaging;

import com.campusbuddies.common.BusinessException;
import com.campusbuddies.common.ErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import com.campusbuddies.user.SysUser;
import com.campusbuddies.user.SysUserMapper;
import com.campusbuddies.user.UserRole;
import com.campusbuddies.user.UserStatus;
import com.campusbuddies.user.VerificationStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {
    private static final int MAX_PAYLOAD_CHARS = 4_096;
    private final Map<Long, Set<WebSocketSession>> sessions = new ConcurrentHashMap<>();
    private final MessageService messages;
    private final ObjectMapper json;
    private final SysUserMapper users;

    public ChatWebSocketHandler(MessageService messages, ObjectMapper json, SysUserMapper users) {
        this.messages = messages;
        this.json = json;
        this.users = users;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        long userId = currentUserId(session);
        sessions.computeIfAbsent(userId, ignored -> ConcurrentHashMap.newKeySet()).add(session);
        send(session, Map.of("type", "CONNECTED"));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage payload) {
        try {
            long userId = currentUserId(session);
            if (payload.getPayloadLength() > MAX_PAYLOAD_CHARS) {
                throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "WebSocket 消息过长");
            }
            JsonNode request = json.readTree(payload.getPayload());
            String type = requiredText(request, "type");
            if ("PING".equals(type)) {
                send(session, Map.of("type", "PONG"));
                return;
            }
            if (!"SEND_MESSAGE".equals(type)) {
                throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "未知 WebSocket 消息类型");
            }
            long conversationId = request.path("conversationId").asLong(0);
            if (conversationId <= 0) throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "conversationId 不合法");
            String clientMessageId = requiredText(request, "clientMessageId");
            MessageType messageType;
            try {
                messageType = MessageType.valueOf(requiredText(request, "messageType"));
            } catch (IllegalArgumentException ex) {
                throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "消息类型不合法");
            }
            String content = request.hasNonNull("content") ? request.get("content").asText() : null;
            Long fileId = request.hasNonNull("fileId") ? request.get("fileId").asLong() : null;
            MessageService.MessageView message = messages.send(userId, conversationId,
                    clientMessageId, messageType, content, fileId);
            send(session, Map.of("type", "ACK", "clientMessageId", clientMessageId,
                    "messageId", message.id(), "duplicate", message.duplicate()));
            if (!message.duplicate()) {
                Map<String, Object> event = Map.of("type", "MESSAGE", "data", message);
                for (Long participantId : messages.participantIds(conversationId)) broadcast(participantId, event);
            }
        } catch (BusinessException ex) {
            sendError(session, ex.errorCode().code(), ex.getMessage());
            if (ex.errorCode() == ErrorCode.UNAUTHENTICATED) closeInvalidSession(session);
        } catch (Exception ex) {
            sendError(session, ErrorCode.INVALID_ARGUMENT.code(), "WebSocket 消息格式不正确");
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Object value = session.getAttributes().get(TicketHandshakeInterceptor.USER_ID_ATTRIBUTE);
        if (value instanceof Long userId) {
            Set<WebSocketSession> userSessions = sessions.get(userId);
            if (userSessions != null) {
                userSessions.remove(session);
                if (userSessions.isEmpty()) sessions.remove(userId, userSessions);
            }
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        if (session.isOpen()) session.close(CloseStatus.SERVER_ERROR);
    }

    private void broadcast(long userId, Object value) {
        Set<WebSocketSession> userSessions = sessions.get(userId);
        if (userSessions == null) return;
        for (WebSocketSession session : userSessions) {
            if (!session.isOpen()) continue;
            try {
                currentUserId(session);
                send(session, value);
            } catch (BusinessException ex) {
                closeInvalidSession(session);
            }
        }
    }

    private void sendError(WebSocketSession session, String code, String message) {
        send(session, Map.of("type", "ERROR", "code", code, "message", message));
    }

    private void send(WebSocketSession session, Object value) {
        if (!session.isOpen()) return;
        try {
            String payload = json.writeValueAsString(value);
            synchronized (session) {
                if (session.isOpen()) session.sendMessage(new TextMessage(payload));
            }
        } catch (IOException ignored) {
            // The close/error callbacks will remove broken sessions.
        }
    }

    private long currentUserId(WebSocketSession session) {
        Object userIdValue = session.getAttributes().get(TicketHandshakeInterceptor.USER_ID_ATTRIBUTE);
        Object versionValue = session.getAttributes().get(TicketHandshakeInterceptor.TOKEN_VERSION_ATTRIBUTE);
        if (!(userIdValue instanceof Long userId) || !(versionValue instanceof Integer tokenVersion)) {
            throw new BusinessException(ErrorCode.UNAUTHENTICATED);
        }
        SysUser user = users.selectById(userId);
        if (user != null && user.getRole() == UserRole.STUDENT && user.getStatus() == UserStatus.ACTIVE
                && user.getVerificationStatus() == VerificationStatus.APPROVED
                && Objects.equals(user.getTokenVersion(), tokenVersion)) return userId;
        throw new BusinessException(ErrorCode.UNAUTHENTICATED);
    }

    private void closeInvalidSession(WebSocketSession session) {
        try {
            if (session.isOpen()) session.close(CloseStatus.POLICY_VIOLATION);
        } catch (IOException ignored) {
            // afterConnectionClosed/transport-error will remove the broken session.
        }
    }

    private String requiredText(JsonNode node, String field) {
        String value = node.hasNonNull(field) ? node.get(field).asText() : null;
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, field + " 不能为空");
        }
        return value;
    }
}
