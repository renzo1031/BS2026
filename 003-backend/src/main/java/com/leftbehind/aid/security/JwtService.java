package com.leftbehind.aid.security;

import com.leftbehind.aid.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Service
public class JwtService {
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private final SecretKey key;
    private final long expirationMinutes;

    public JwtService(AppProperties properties) {
        byte[] secret = properties.jwt().secret().getBytes(StandardCharsets.UTF_8);
        if (secret.length < 32) {
            throw new IllegalStateException("JWT_SECRET must contain at least 32 bytes");
        }
        this.key = Keys.hmacShaKeyFor(secret);
        this.expirationMinutes = properties.jwt().expirationMinutes();
    }

    public Token issue(Long userId, String sessionId) {
        Instant now = Instant.now();
        Instant expires = now.plusSeconds(expirationMinutes * 60);
        String token = Jwts.builder()
                .subject(userId.toString())
                .claim("sid", sessionId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expires))
                .signWith(key)
                .compact();
        return new Token(token, LocalDateTime.ofInstant(expires, ZONE));
    }

    public ParsedToken parse(String token) {
        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        return new ParsedToken(Long.valueOf(claims.getSubject()), claims.get("sid", String.class));
    }

    public record Token(String value, LocalDateTime expiresAt) {
    }

    public record ParsedToken(Long userId, String sessionId) {
    }
}
