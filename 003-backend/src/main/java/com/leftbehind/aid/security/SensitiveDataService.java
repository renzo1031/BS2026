package com.leftbehind.aid.security;

import com.leftbehind.aid.config.AppProperties;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class SensitiveDataService {
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH = 128;
    private final SecureRandom random = new SecureRandom();
    private final SecretKeySpec key;

    public SensitiveDataService(AppProperties properties) {
        byte[] decoded = Base64.getDecoder().decode(properties.pii().key());
        if (decoded.length != 32) {
            throw new IllegalStateException("PII_ENCRYPTION_KEY must be a Base64-encoded 32-byte key");
        }
        this.key = new SecretKeySpec(decoded, "AES");
    }

    public String encrypt(String plainText) {
        if (plainText == null || plainText.isBlank()) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH, iv));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return "v1:" + Base64.getEncoder().encodeToString(
                    ByteBuffer.allocate(iv.length + encrypted.length).put(iv).put(encrypted).array());
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Sensitive data encryption failed", exception);
        }
    }

    public String decrypt(String cipherText) {
        if (cipherText == null || cipherText.isBlank()) {
            return null;
        }
        if (!cipherText.startsWith("v1:")) {
            throw new IllegalStateException("Unsupported sensitive data format");
        }
        try {
            byte[] payload = Base64.getDecoder().decode(cipherText.substring(3));
            if (payload.length <= IV_LENGTH) {
                throw new IllegalStateException("Invalid sensitive data payload");
            }
            byte[] iv = new byte[IV_LENGTH];
            byte[] encrypted = new byte[payload.length - IV_LENGTH];
            System.arraycopy(payload, 0, iv, 0, IV_LENGTH);
            System.arraycopy(payload, IV_LENGTH, encrypted, 0, encrypted.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            throw new IllegalStateException("Sensitive data decryption failed", exception);
        }
    }

    public String maskName(String name) {
        return name == null || name.isBlank() ? "未填写" : name.substring(0, 1) + "**";
    }

    public String maskPhone(String phone) {
        return phone == null || phone.length() < 7 ? "未填写" : phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
