package com.campusbuddies.security;

import com.campusbuddies.config.AppProperties;
import com.campusbuddies.user.SysUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class JwtService {
    public enum TokenType { ACCESS, REFRESH }

    public record Token(String value, String id, Instant expiresAt, TokenType type) {}
    public record Parsed(long userId, String id, Instant expiresAt, int tokenVersion, TokenType type) {}

    private final SecretKey key;
    private final long accessMinutes;

    public JwtService(AppProperties.Jwt properties) {
        String secret = properties.secret();
        if (!StringUtils.hasText(secret) || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT_SECRET 必须通过环境变量提供且至少 32 字节");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessMinutes = properties.accessTokenMinutes();
    }

    public Token issueAccess(SysUser user) { return issue(user, TokenType.ACCESS, accessMinutes, ChronoUnit.MINUTES); }
    public Token issueRefresh(SysUser user) { return issue(user, TokenType.REFRESH, 14, ChronoUnit.DAYS); }

    private Token issue(SysUser user, TokenType type, long amount, ChronoUnit unit) {
        Instant now = Instant.now();
        Instant expires = now.plus(amount, unit);
        String id = UUID.randomUUID().toString();
        String value = Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .id(id)
                .claim("typ", type.name())
                .claim("ver", user.getTokenVersion() == null ? 0 : user.getTokenVersion())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expires))
                .signWith(key)
                .compact();
        return new Token(value, id, expires, type);
    }

    public Parsed parse(String token) {
        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        return new Parsed(
                Long.parseLong(claims.getSubject()),
                claims.getId(),
                claims.getExpiration().toInstant(),
                claims.get("ver", Integer.class),
                TokenType.valueOf(claims.get("typ", String.class)));
    }
}
