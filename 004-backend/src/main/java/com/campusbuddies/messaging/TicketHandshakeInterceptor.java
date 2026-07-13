package com.campusbuddies.messaging;

import java.util.Map;
import java.util.Objects;
import com.campusbuddies.user.SysUser;
import com.campusbuddies.user.SysUserMapper;
import com.campusbuddies.user.UserRole;
import com.campusbuddies.user.UserStatus;
import com.campusbuddies.user.VerificationStatus;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class TicketHandshakeInterceptor implements HandshakeInterceptor {
    public static final String USER_ID_ATTRIBUTE = "wsUserId";
    public static final String TOKEN_VERSION_ATTRIBUTE = "wsTokenVersion";
    private final WsTicketStore tickets;
    private final SysUserMapper users;

    public TicketHandshakeInterceptor(WsTicketStore tickets, SysUserMapper users) {
        this.tickets = tickets;
        this.users = users;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String ticket = UriComponentsBuilder.fromUri(request.getURI()).build()
                .getQueryParams().getFirst("ticket");
        WsTicketStore.TicketPrincipal principal = tickets.consume(ticket);
        SysUser user = principal == null ? null : users.selectById(principal.userId());
        if (user == null || user.getRole() != UserRole.STUDENT || user.getStatus() != UserStatus.ACTIVE
                || user.getVerificationStatus() != VerificationStatus.APPROVED
                || !Objects.equals(user.getTokenVersion(), principal.tokenVersion())) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
        attributes.put(USER_ID_ATTRIBUTE, principal.userId());
        attributes.put(TOKEN_VERSION_ATTRIBUTE, principal.tokenVersion());
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
    }
}
