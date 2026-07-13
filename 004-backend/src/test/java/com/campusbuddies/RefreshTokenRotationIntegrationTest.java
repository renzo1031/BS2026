package com.campusbuddies;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campusbuddies.security.InMemoryTokenRevocationStore;
import com.campusbuddies.security.RedisTokenRevocationStore;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RefreshTokenRotationIntegrationTest {
    @Autowired
    private TestRestTemplate rest;

    @Test
    void concurrentRefreshConsumesTokenExactlyOnce() throws Exception {
        ResponseEntity<Map> login = rest.postForEntity("/api/v1/auth/wechat-login",
                Map.of("code", "refresh-atomic-" + UUID.randomUUID()), Map.class);
        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(login.getBody()).isNotNull();
        @SuppressWarnings("unchecked")
        Map<String, Object> loginData = (Map<String, Object>) login.getBody().get("data");
        String refreshToken = (String) loginData.get("refreshToken");

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<HttpStatus>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < 2; i++) {
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    if (!start.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("并发刷新未按时开始");
                    ResponseEntity<Map> response = rest.postForEntity("/api/v1/auth/refresh",
                            Map.of("refreshToken", refreshToken), Map.class);
                    return HttpStatus.valueOf(response.getStatusCode().value());
                }));
            }
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            assertThat(List.of(futures.get(0).get(10, TimeUnit.SECONDS),
                    futures.get(1).get(10, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(HttpStatus.OK, HttpStatus.UNAUTHORIZED);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void inMemoryConsumeIsAtomic() throws Exception {
        InMemoryTokenRevocationStore store = new InMemoryTokenRevocationStore();
        String tokenId = UUID.randomUUID().toString();
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Boolean> first = pool.submit(() -> {
                ready.countDown();
                start.await(5, TimeUnit.SECONDS);
                return store.consume(tokenId, Instant.now().plusSeconds(60));
            });
            Future<Boolean> second = pool.submit(() -> {
                ready.countDown();
                start.await(5, TimeUnit.SECONDS);
                return store.consume(tokenId, Instant.now().plusSeconds(60));
            });
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            assertThat(List.of(first.get(5, TimeUnit.SECONDS), second.get(5, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(true, false);
            assertThat(store.isRevoked(tokenId)).isTrue();
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void redisConsumeUsesSingleAtomicSetIfAbsent() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.setIfAbsent(eq("auth:revoked:refresh-id"), eq("1"), any(Duration.class)))
                .thenReturn(true, false);
        RedisTokenRevocationStore store = new RedisTokenRevocationStore(redis);
        Instant expiresAt = Instant.now().plusSeconds(60);

        assertThat(store.consume("refresh-id", expiresAt)).isTrue();
        assertThat(store.consume("refresh-id", expiresAt)).isFalse();

        verify(values, org.mockito.Mockito.times(2))
                .setIfAbsent(eq("auth:revoked:refresh-id"), eq("1"), any(Duration.class));
        verify(redis, never()).hasKey(anyString());
    }
}
