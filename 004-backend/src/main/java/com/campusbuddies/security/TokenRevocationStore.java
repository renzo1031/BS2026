package com.campusbuddies.security;

import java.time.Instant;

public interface TokenRevocationStore {
    void revoke(String tokenId, Instant expiresAt);
    boolean isRevoked(String tokenId);

    /** Atomically marks an unexpired one-time token as used. */
    boolean consume(String tokenId, Instant expiresAt);
}
