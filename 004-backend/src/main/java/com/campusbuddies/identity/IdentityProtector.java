package com.campusbuddies.identity;

import com.campusbuddies.config.AppProperties;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class IdentityProtector {
    private static final int NONCE_BYTES = 12;
    private final SecretKey hmacKey;
    private final SecretKey encryptionKey;
    private final SecureRandom random = new SecureRandom();

    public IdentityProtector(AppProperties.Identity properties) {
        if (!StringUtils.hasText(properties.hmacSecret()) || properties.hmacSecret().length() < 32
                || !StringUtils.hasText(properties.encryptionSecret()) || properties.encryptionSecret().length() < 32) {
            throw new IllegalStateException("身份 HMAC 与加密密钥必须通过环境变量提供且至少 32 字符");
        }
        this.hmacKey = new SecretKeySpec(properties.hmacSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        this.encryptionKey = new SecretKeySpec(sha256(properties.encryptionSecret()), "AES");
    }

    public String normalize(String value) {
        if (!StringUtils.hasText(value)) throw new IllegalArgumentException("校园标识不能为空");
        String normalized = value.trim().replaceAll("\\s+", "").toUpperCase(java.util.Locale.ROOT);
        if (normalized.length() < 4 || normalized.length() > 64) throw new IllegalArgumentException("校园标识长度必须在 4 到 64 之间");
        return normalized;
    }

    public String fingerprint(long campusId, String type, String normalized) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(hmacKey);
            return HexFormat.of().formatHex(mac.doFinal((campusId + ":" + type + ":" + normalized).getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("无法保护校园标识", ex);
        }
    }

    public String encrypt(long campusId, String type, String normalized) {
        try {
            byte[] nonce = new byte[NONCE_BYTES];
            random.nextBytes(nonce);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, new GCMParameterSpec(128, nonce));
            cipher.updateAAD((campusId + ":" + type).getBytes(StandardCharsets.UTF_8));
            byte[] encrypted = cipher.doFinal(normalized.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(ByteBuffer.allocate(nonce.length + encrypted.length).put(nonce).put(encrypted).array());
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("无法加密校园标识", ex);
        }
    }

    public String decrypt(long campusId, String type, String ciphertext) {
        try {
            byte[] all = Base64.getDecoder().decode(ciphertext);
            if (all.length <= NONCE_BYTES) throw new GeneralSecurityException("ciphertext too short");
            byte[] nonce = java.util.Arrays.copyOfRange(all, 0, NONCE_BYTES);
            byte[] encrypted = java.util.Arrays.copyOfRange(all, NONCE_BYTES, all.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, encryptionKey, new GCMParameterSpec(128, nonce));
            cipher.updateAAD((campusId + ":" + type).getBytes(StandardCharsets.UTF_8));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException ex) {
            throw new IllegalStateException("无法解密校园标识", ex);
        }
    }

    public String mask(String normalized) {
        return "*".repeat(Math.max(4, normalized.length() - 4)) + normalized.substring(normalized.length() - 4);
    }

    private byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
