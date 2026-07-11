package com.leftbehind.aid.service;

import com.leftbehind.aid.common.BusinessException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BusinessRulesTest {
    @Test
    void enforcesAllowedState() {
        assertDoesNotThrow(() -> BusinessRules.requireState("DRAFT", "DRAFT", "REJECTED"));
        assertThrows(BusinessException.class,
                () -> BusinessRules.requireState("COMPLETED", "DRAFT", "REJECTED"));
    }

    @Test
    void protectsPublicSummaryFromDirectIdentifiers() {
        assertDoesNotThrow(() -> BusinessRules.requirePublicSummarySafe("需要每周一次学习陪伴服务"));
        assertThrows(BusinessException.class,
                () -> BusinessRules.requirePublicSummarySafe("请联系13800138000"));
        assertThrows(BusinessException.class,
                () -> BusinessRules.requirePublicSummarySafe("证件号11010120100101123X"));
    }

    @Test
    void storesReviewCommentOnlyAsARejectionReason() {
        assertNull(BusinessRules.rejectionReason("APPROVED", "档案信息完整"));
        assertEquals("材料不完整", BusinessRules.rejectionReason("REJECTED", "  材料不完整  "));
        assertThrows(BusinessException.class,
                () -> BusinessRules.rejectionReason("REJECTED", " "));
    }
}
