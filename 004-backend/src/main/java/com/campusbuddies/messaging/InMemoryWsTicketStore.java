package com.campusbuddies.messaging;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"test", "local"})
public class InMemoryWsTicketStore implements WsTicketStore {
    private record Ticket(long userId, int tokenVersion, Instant expiresAt) {}
    private final Map<String, Ticket> tickets = new ConcurrentHashMap<>();

    @Override
    public String issue(long userId, int tokenVersion) {
        String value = UUID.randomUUID().toString();
        tickets.put(value, new Ticket(userId, tokenVersion, Instant.now().plusSeconds(60)));
        return value;
    }

    @Override
    public TicketPrincipal consume(String ticket) {
        Ticket value = tickets.remove(ticket);
        return value == null || value.expiresAt().isBefore(Instant.now())
                ? null : new TicketPrincipal(value.userId(), value.tokenVersion());
    }
}
