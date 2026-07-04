package com.campus.lostfound.security;

import com.campus.lostfound.common.BizException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Component
public class JwtUtil {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.jwt.secret:campus-lost-found-secret}")
    private String secret;

    @Value("${app.jwt.expire-seconds:86400}")
    private long expireSeconds;

    public String createToken(CurrentUser user) {
        try {
            Map<String, Object> header = Map.of("alg", "HS256", "typ", "JWT");
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("uid", user.id());
            payload.put("username", user.username());
            payload.put("realName", user.realName());
            payload.put("roles", user.roles());
            payload.put("exp", Instant.now().getEpochSecond() + expireSeconds);

            String headerPart = encode(objectMapper.writeValueAsBytes(header));
            String payloadPart = encode(objectMapper.writeValueAsBytes(payload));
            String signature = sign(headerPart + "." + payloadPart);
            return headerPart + "." + payloadPart + "." + signature;
        } catch (Exception ex) {
            throw new BizException("生成 token 失败");
        }
    }

    public CurrentUser parse(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw new BizException(401, "无效 token");
            }
            String expected = sign(parts[0] + "." + parts[1]);
            if (!expected.equals(parts[2])) {
                throw new BizException(401, "无效 token 签名");
            }
            Map<String, Object> payload = objectMapper.readValue(Base64.getUrlDecoder().decode(parts[1]), new TypeReference<>() {
            });
            long exp = Long.parseLong(String.valueOf(payload.get("exp")));
            if (Instant.now().getEpochSecond() > exp) {
                throw new BizException(401, "token 已过期");
            }
            Long uid = Long.valueOf(String.valueOf(payload.get("uid")));
            String username = String.valueOf(payload.get("username"));
            String realName = String.valueOf(payload.get("realName"));
            @SuppressWarnings("unchecked")
            Set<String> roles = Set.copyOf((java.util.List<String>) payload.get("roles"));
            return new CurrentUser(uid, username, realName, roles);
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BizException(401, "token 解析失败");
        }
    }

    private String encode(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sign(String content) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return encode(mac.doFinal(content.getBytes(StandardCharsets.UTF_8)));
    }
}
