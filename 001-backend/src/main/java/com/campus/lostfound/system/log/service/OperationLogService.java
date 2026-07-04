package com.campus.lostfound.system.log.service;

import com.campus.lostfound.common.IdGenerator;
import com.campus.lostfound.security.CurrentUser;
import com.campus.lostfound.security.LoginContext;
import com.campus.lostfound.system.log.entity.SysOperationLog;
import com.campus.lostfound.system.log.mapper.SysOperationLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OperationLogService {
    private final SysOperationLogMapper logMapper;

    public void record(String targetType, Long targetId, String action, String beforeStatus, String afterStatus, String reason) {
        SysOperationLog log = new SysOperationLog();
        log.setId(IdGenerator.nextId());
        CurrentUser user = null;
        try {
            user = LoginContext.get();
        } catch (Exception ignored) {
            // anonymous system event
        }
        if (user != null) {
            log.setOperatorId(user.id());
            log.setOperatorName(user.realName());
            log.setOperatorRole(user.primaryRole());
        }
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setAction(action);
        log.setBeforeStatus(beforeStatus);
        log.setAfterStatus(afterStatus);
        log.setResult("SUCCESS");
        log.setReason(reason);
        log.setCreatedAt(LocalDateTime.now());

        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            log.setRequestIp(request.getRemoteAddr());
            log.setUserAgent(request.getHeader("User-Agent"));
            log.setRequestPath(request.getRequestURI());
        }
        logMapper.insert(log);
    }
}
