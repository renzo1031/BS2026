package com.campus.lostfound.lostfound.item;

import java.util.Map;
import java.util.Set;

public final class ItemStateMachine {
    private static final Map<String, Set<String>> ALLOWED = Map.of(
            "DRAFT", Set.of("PENDING_REVIEW"),
            "PENDING_REVIEW", Set.of("PUBLISHED", "REJECTED", "DRAFT"),
            "REJECTED", Set.of("PENDING_REVIEW"),
            "PUBLISHED", Set.of("CLAIM_REVIEWING", "OFFLINE", "ARCHIVED"),
            "CLAIM_REVIEWING", Set.of("PUBLISHED", "HANDOVER_PENDING"),
            "HANDOVER_PENDING", Set.of("COMPLETED"),
            "COMPLETED", Set.of("ARCHIVED"),
            "OFFLINE", Set.of("PUBLISHED", "ARCHIVED")
    );

    private ItemStateMachine() {
    }

    public static void assertTransition(String before, String after) {
        if (before == null || after == null) {
            throw new IllegalArgumentException("状态不能为空");
        }
        if (before.equals(after)) {
            return;
        }
        if (!ALLOWED.getOrDefault(before, Set.of()).contains(after)) {
            throw new IllegalArgumentException("非法状态流转：" + before + " -> " + after);
        }
    }
}
