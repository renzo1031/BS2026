package com.campusbuddies.security;

import com.campusbuddies.user.SysUser;
import com.campusbuddies.user.SysUserMapper;
import com.campusbuddies.user.UserStatus;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private final JwtService jwtService;
    private final TokenRevocationStore revocations;
    private final SysUserMapper users;

    public JwtAuthenticationFilter(JwtService jwtService, TokenRevocationStore revocations, SysUserMapper users) {
        this.jwtService = jwtService;
        this.revocations = revocations;
        this.users = users;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            authenticate(header.substring(7));
        }
        chain.doFilter(request, response);
    }

    private void authenticate(String token) {
        try {
            JwtService.Parsed parsed = jwtService.parse(token);
            if (parsed.type() != JwtService.TokenType.ACCESS || revocations.isRevoked(parsed.id())) return;
            SysUser user = users.selectById(parsed.userId());
            if (user == null || user.getStatus() == UserStatus.CLOSED) return;
            int currentVersion = user.getTokenVersion() == null ? 0 : user.getTokenVersion();
            if (currentVersion != parsed.tokenVersion()) return;
            AuthPrincipal principal = new AuthPrincipal(
                    user.getId(), user.getCampusId(), user.getRole(), user.getStatus(), user.getVerificationStatus(), currentVersion,
                    parsed.id(), parsed.expiresAt().getEpochSecond());
            var auth = new UsernamePasswordAuthenticationToken(
                    principal, token, List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
            SecurityContextHolder.getContext().setAuthentication(auth);
        } catch (RuntimeException ex) {
            SecurityContextHolder.clearContext();
            log.debug("JWT authentication rejected: {}", ex.getClass().getSimpleName());
        }
    }
}
