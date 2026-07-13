package com.campusbuddies.messaging;

import java.time.Duration;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@Profile("!test & !local")
public class RedisWsTicketStore implements WsTicketStore {
    private static final String PREFIX = "ws:ticket:";
    private final StringRedisTemplate redis;

    public RedisWsTicketStore(StringRedisTemplate redis) { this.redis = redis; }

    @Override
    public String issue(long userId, int tokenVersion) {
        String ticket = UUID.randomUUID().toString();
        redis.opsForValue().set(PREFIX + ticket, userId + ":" + tokenVersion, Duration.ofSeconds(60));
        return ticket;
    }

    @Override
    public TicketPrincipal consume(String ticket) {
        if (ticket == null || ticket.length() > 64) return null;
        String value = redis.opsForValue().getAndDelete(PREFIX + ticket);
        try {
            if (value == null) return null;
            String[] parts = value.split(":", -1);
            if (parts.length != 2) return null;
            return new TicketPrincipal(Long.parseLong(parts[0]), Integer.parseInt(parts[1]));
        } catch (NumberFormatException | IndexOutOfBoundsException ex) {
            return null;
        }
    }
}
