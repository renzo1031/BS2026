package com.campus.service.config;

import com.campus.service.common.AuthContext;
import com.campus.service.common.BusinessException;
import com.campus.service.common.JwtUtil;
import com.campus.service.common.RequireRole;
import com.campus.service.entity.User;
import com.campus.service.mapper.UserMapper;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;

@Component
public class AuthInterceptor implements HandlerInterceptor {
    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;

    public AuthInterceptor(JwtUtil jwtUtil, UserMapper userMapper) {
        this.jwtUtil = jwtUtil;
        this.userMapper = userMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        AuthContext.clear();
        if ("OPTIONS".equalsIgnoreCase(request.getMethod()) || !(handler instanceof HandlerMethod method)) {
            return true;
        }
        String path = request.getRequestURI();
        if (path.startsWith("/api/auth/login") || path.startsWith("/api/auth/register") || path.startsWith("/api/catalog")) {
            return true;
        }
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            throw new BusinessException(401, "请先登录");
        }
        try {
            AuthContext.AuthUser user = jwtUtil.parse(auth.substring(7));
            User current = userMapper.selectById(user.userId());
            if (current == null || current.getDeleted() == 1 || !"ENABLED".equals(current.getStatus())
                    || !java.util.Objects.equals(current.getTokenVersion(), user.tokenVersion())) {
                throw new BusinessException(401, "登录状态已失效，请重新登录");
            }
            AuthContext.set(user);
            RequireRole required = method.getMethodAnnotation(RequireRole.class);
            if (required == null) {
                required = method.getBeanType().getAnnotation(RequireRole.class);
            }
            if (required != null && Arrays.stream(required.value()).noneMatch(user::hasRole)) {
                throw new BusinessException(403, "无权限操作");
            }
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            AuthContext.clear();
            throw new BusinessException(401, "登录状态已失效，请重新登录");
        } catch (RuntimeException e) {
            AuthContext.clear();
            throw e;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        AuthContext.clear();
    }
}
