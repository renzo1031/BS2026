package com.campusbuddies.messaging;

import com.campusbuddies.common.BusinessException;
import com.campusbuddies.common.ErrorCode;
import com.campusbuddies.config.AppProperties;
import org.springframework.stereotype.Component;

@Component
public class FailClosedContentSafetyGateway implements ContentSafetyGateway {
    private final boolean developmentMode;

    public FailClosedContentSafetyGateway(AppProperties.Wechat properties) {
        this.developmentMode = properties.devLoginEnabled();
    }

    @Override
    public void validateText(String content) {
        if (!developmentMode) {
            throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_UNAVAILABLE, "微信文本内容安全能力尚未配置");
        }
        for (int index = 0; index < content.length(); index++) {
            char value = content.charAt(index);
            if (Character.isISOControl(value) && value != '\n' && value != '\t') {
                throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "消息包含不支持的控制字符");
            }
        }
    }
}
