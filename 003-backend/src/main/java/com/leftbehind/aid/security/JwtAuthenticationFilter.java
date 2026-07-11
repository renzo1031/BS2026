package com.leftbehind.aid.security;

import com.leftbehind.aid.domain.Domain;
import com.leftbehind.aid.mapper.SystemMapper;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final SystemMapper systemMapper;

    public JwtAuthenticationFilter(JwtService jwtService, SystemMapper systemMapper) {
        this.jwtService = jwtService;
        this.systemMapper = systemMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            authenticate(authorization.substring(7), request);
        }
        filterChain.doFilter(request, response);
    }

    private void authenticate(String token, HttpServletRequest request) {
        try {
            JwtService.ParsedToken parsed = jwtService.parse(token);
            if (parsed.sessionId() == null || systemMapper.isActiveSession(parsed.sessionId(), parsed.userId()) != 1) {
                return;
            }
            Domain.UserRow user = systemMapper.findUserById(parsed.userId());
            if (user == null || !"ACTIVE".equals(user.status())) {
                return;
            }
            Set<String> permissions = new LinkedHashSet<>(systemMapper.findPermissionCodes(user.roleId()));
            PlatformPrincipal principal = new PlatformPrincipal(
                    user.id(), user.departmentId(), user.username(), user.displayName(), user.roleCode(),
                    user.dataScope(), Set.copyOf(permissions), parsed.sessionId());
            Set<SimpleGrantedAuthority> authorities = new LinkedHashSet<>();
            authorities.add(new SimpleGrantedAuthority("ROLE_" + user.roleCode()));
            permissions.forEach(permission -> authorities.add(new SimpleGrantedAuthority(permission)));
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(principal, null, authorities);
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (JwtException | IllegalArgumentException ignored) {
            SecurityContextHolder.clearContext();
        }
    }
}
