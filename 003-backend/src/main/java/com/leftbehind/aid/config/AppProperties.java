package com.leftbehind.aid.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Jwt jwt, Pii pii, Cors cors) {
    public record Jwt(String secret, long expirationMinutes) {
    }

    public record Pii(String key) {
    }

    public record Cors(String allowedOrigins) {
    }
}
