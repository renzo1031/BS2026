package com.campusbuddies.security;

import java.time.Duration;
import java.time.Instant;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test & !local")
public class RedisTokenRevocationStore implements TokenRevocationStore {
    private static final String PREFIX = "auth:revoked:";
    private final StringRedisTemplate redis;

    public RedisTokenRevocationStore(StringRedisTemplate redis) { this.redis = redis; }

    @Override
    public void revoke(String tokenId, Instant expiresAt) {
        Duration ttl = Duration.between(Instant.now(), expiresAt);
        if (ttl.isNegative() || ttl.isZero()) return;
        redis.opsForValue().set(PREFIX + tokenId, "1", ttl);
    }

    @Override
    public boolean isRevoked(String tokenId) {
        return Boolean.TRUE.equals(redis.hasKey(PREFIX + tokenId));
    }

    @Override
    public boolean consume(String tokenId, Instant expiresAt) {
        Duration ttl = Duration.between(Instant.now(), expiresAt);
        if (ttl.isNegative() || ttl.isZero()) return false;
        return Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(PREFIX + tokenId, "1", ttl));
    }
}
