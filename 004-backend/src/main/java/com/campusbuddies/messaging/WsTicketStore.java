package com.campusbuddies.messaging;

public interface WsTicketStore {
    record TicketPrincipal(long userId, int tokenVersion) {}

    String issue(long userId, int tokenVersion);
    TicketPrincipal consume(String ticket);
}
