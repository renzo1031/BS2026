package com.campus.service.service;

import com.campus.service.common.BusinessException;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginAttemptService {
    private static final int MAX_FAILURES = 5;
    private static final int MAX_ENTRIES = 10_000;
    private static final Duration LOCK_TIME = Duration.ofMinutes(5);

    private final ConcurrentHashMap<String, Attempt> attempts = new ConcurrentHashMap<>();
    private final Clock clock;

    public LoginAttemptService() {
        this(Clock.systemUTC());
    }

    LoginAttemptService(Clock clock) {
        this.clock = clock;
    }

    public void checkAllowed(String username) {
        String key = key(username);
        Attempt attempt = attempts.get(key);
        if (attempt == null) {
            return;
        }
        Instant now = clock.instant();
        if (attempt.lockedUntil() != null && attempt.lockedUntil().isAfter(now)) {
            throw invalidCredentials();
        }
        if (attempt.lockedUntil() != null || attempt.lastFailure().plus(LOCK_TIME).isBefore(now)) {
            attempts.remove(key, attempt);
        }
    }

    public void recordFailure(String username) {
        String key = key(username);
        if (attempts.size() >= MAX_ENTRIES && !attempts.containsKey(key)) {
            attempts.keys().asIterator().forEachRemaining(candidate -> {
                if (attempts.size() >= MAX_ENTRIES) {
                    attempts.remove(candidate);
                }
            });
        }
        Instant now = clock.instant();
        attempts.compute(key, (ignored, old) -> {
            int failures = old == null ? 1 : old.failures() + 1;
            Instant lockedUntil = failures >= MAX_FAILURES ? now.plus(LOCK_TIME) : null;
            return new Attempt(failures, lockedUntil, now);
        });
    }

    public void recordSuccess(String username) {
        attempts.remove(key(username));
    }

    private String key(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }

    private BusinessException invalidCredentials() {
        return new BusinessException(401, "用户名或密码错误");
    }

    private record Attempt(int failures, Instant lockedUntil, Instant lastFailure) {
    }
}
