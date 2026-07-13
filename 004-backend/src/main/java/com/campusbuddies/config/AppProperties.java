package com.campusbuddies.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

public final class AppProperties {
    private AppProperties() {}

    @ConfigurationProperties("campus-buddy.jwt")
    public record Jwt(String secret, long accessTokenMinutes) {}

    @ConfigurationProperties("campus-buddy.identity")
    public record Identity(String hmacSecret, String encryptionSecret) {}

    @ConfigurationProperties("campus-buddy.wechat")
    public record Wechat(String appId, String appSecret, boolean devLoginEnabled) {}

    @ConfigurationProperties("campus-buddy.minio")
    public record Minio(String endpoint, String accessKey, String secretKey, String bucket) {}

    @ConfigurationProperties("campus-buddy.cors")
    public record Cors(String allowedOrigins) {}
}
