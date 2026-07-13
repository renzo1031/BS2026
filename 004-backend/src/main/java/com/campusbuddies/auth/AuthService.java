package com.campusbuddies.auth;

import com.campusbuddies.common.BusinessException;
import com.campusbuddies.common.ErrorCode;
import com.campusbuddies.config.AppProperties;
import com.campusbuddies.security.AuthPrincipal;
import com.campusbuddies.security.JwtService;
import com.campusbuddies.security.TokenRevocationStore;
import com.campusbuddies.user.SysUser;
import com.campusbuddies.user.SysUserMapper;
import com.campusbuddies.user.UserRole;
import com.campusbuddies.user.UserStatus;
import com.campusbuddies.user.VerificationStatus;
import java.util.Map;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Service
public class AuthService {
    public record Tokens(String accessToken, String refreshToken, long expiresAt, UserView user) {}
    public record UserView(long id, Long campusId, String nickname, UserRole role, UserStatus status,
                           VerificationStatus verificationStatus) {
        static UserView from(SysUser user) {
            return new UserView(user.getId(), user.getCampusId(), user.getNickname(), user.getRole(),
                    user.getStatus(), user.getVerificationStatus());
        }
    }

    private final SysUserMapper users;
    private final PasswordEncoder passwords;
    private final JwtService jwt;
    private final TokenRevocationStore revocations;
    private final AppProperties.Wechat wechat;
    private final RestClient rest = RestClient.create();

    public AuthService(SysUserMapper users, PasswordEncoder passwords, JwtService jwt,
                       TokenRevocationStore revocations, AppProperties.Wechat wechat) {
        this.users = users;
        this.passwords = passwords;
        this.jwt = jwt;
        this.revocations = revocations;
        this.wechat = wechat;
    }

    public Tokens adminLogin(String username, String password) {
        SysUser user = users.findByUsername(username);
        if (user == null || user.getPasswordHash() == null || !passwords.matches(password, user.getPasswordHash())
                || user.getStatus() != UserStatus.ACTIVE || user.getRole() == UserRole.STUDENT) {
            throw new BusinessException(ErrorCode.UNAUTHENTICATED, "用户名或密码错误");
        }
        return issue(user);
    }

    @Transactional
    public Tokens wechatLogin(String code) {
        String openid = resolveOpenid(code);
        SysUser user = users.findByWechatOpenid(openid);
        if (user == null) {
            user = new SysUser();
            user.setWechatOpenid(openid);
            user.setNickname("新同学");
            user.setRole(UserRole.STUDENT);
            user.setStatus(UserStatus.ACTIVE);
            user.setVerificationStatus(VerificationStatus.UNVERIFIED);
            user.setTokenVersion(0);
            users.insert(user);
        }
        if (user.getStatus() == UserStatus.CLOSED) throw new BusinessException(ErrorCode.FORBIDDEN, "账号已关闭");
        return issue(user);
    }

    public Tokens refresh(String refreshToken) {
        try {
            JwtService.Parsed parsed = jwt.parse(refreshToken);
            if (parsed.type() != JwtService.TokenType.REFRESH) {
                throw new BusinessException(ErrorCode.TOKEN_INVALID);
            }
            SysUser user = users.selectById(parsed.userId());
            int version = user == null || user.getTokenVersion() == null ? -1 : user.getTokenVersion();
            if (user == null || user.getStatus() == UserStatus.CLOSED || version != parsed.tokenVersion()) {
                throw new BusinessException(ErrorCode.TOKEN_INVALID);
            }
            if (!revocations.consume(parsed.id(), parsed.expiresAt())) {
                throw new BusinessException(ErrorCode.TOKEN_INVALID);
            }
            return issue(user);
        } catch (BusinessException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }
    }

    @Transactional
    public void logout(AuthPrincipal principal, String refreshToken) {
        revocations.revoke(principal.tokenId(), java.time.Instant.ofEpochSecond(principal.tokenExpiresAtEpochSecond()));
        if (StringUtils.hasText(refreshToken)) {
            try {
                JwtService.Parsed parsed = jwt.parse(refreshToken);
                if (parsed.userId() == principal.userId()) revocations.revoke(parsed.id(), parsed.expiresAt());
            } catch (RuntimeException ignored) {
                // Invalid refresh tokens are already unusable.
            }
        }
        users.incrementTokenVersion(principal.userId());
    }

    private Tokens issue(SysUser user) {
        JwtService.Token access = jwt.issueAccess(user);
        JwtService.Token refresh = jwt.issueRefresh(user);
        return new Tokens(access.value(), refresh.value(), access.expiresAt().getEpochSecond(), UserView.from(user));
    }

    private String resolveOpenid(String code) {
        if (!StringUtils.hasText(code)) throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "微信登录 code 不能为空");
        if (wechat.devLoginEnabled()) return "dev-openid-" + code.trim();
        if (!StringUtils.hasText(wechat.appId()) || !StringUtils.hasText(wechat.appSecret())) {
            throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_UNAVAILABLE, "微信登录尚未配置");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> body = rest.get()
                .uri(uri -> uri.scheme("https").host("api.weixin.qq.com").path("/sns/jscode2session")
                        .queryParam("appid", wechat.appId()).queryParam("secret", wechat.appSecret())
                        .queryParam("js_code", code).queryParam("grant_type", "authorization_code").build())
                .retrieve().body(Map.class);
        String openid = body == null ? null : (String) body.get("openid");
        if (!StringUtils.hasText(openid)) throw new BusinessException(ErrorCode.UNAUTHENTICATED, "微信登录失败");
        return openid;
    }
}
