package com.campusbuddies.security;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"test", "local"})
public class InMemoryTokenRevocationStore implements TokenRevocationStore {
    // ponytail: local profile keeps revocation in process; use Redis when running multiple backend instances.
    private final Map<String, Instant> revoked = new ConcurrentHashMap<>();

    @Override
    public void revoke(String tokenId, Instant expiresAt) { revoked.put(tokenId, expiresAt); }

    @Override
    public boolean isRevoked(String tokenId) {
        Instant expiresAt = revoked.get(tokenId);
        if (expiresAt == null) return false;
        if (!expiresAt.isAfter(Instant.now())) {
            revoked.remove(tokenId, expiresAt);
            return false;
        }
        return true;
    }

    @Override
    public boolean consume(String tokenId, Instant expiresAt) {
        Instant now = Instant.now();
        if (!expiresAt.isAfter(now)) return false;
        AtomicBoolean consumed = new AtomicBoolean();
        revoked.compute(tokenId, (key, currentExpiry) -> {
            if (currentExpiry != null && currentExpiry.isAfter(now)) return currentExpiry;
            consumed.set(true);
            return expiresAt;
        });
        return consumed.get();
    }
}
