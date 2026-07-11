package com.leftbehind.aid.service;

import com.leftbehind.aid.mapper.SystemMapper;
import com.leftbehind.aid.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class AuditService {
    private final SystemMapper systemMapper;

    public AuditService(SystemMapper systemMapper) {
        this.systemMapper = systemMapper;
    }

    public void log(String action, String businessType, Object businessId,
                    String beforeStatus, String afterStatus, String detail) {
        log(SecurityUtils.current().id(), action, businessType, businessId, beforeStatus, afterStatus, detail);
    }

    public void log(Long userId, String action, String businessType, Object businessId,
                    String beforeStatus, String afterStatus, String detail) {
        systemMapper.insertAudit(userId, action, businessType,
                businessId == null ? null : businessId.toString(), beforeStatus, afterStatus, detail, remoteAddress());
    }

    private String remoteAddress() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            HttpServletRequest request = attributes.getRequest();
            return request.getRemoteAddr();
        }
        return null;
    }
}
