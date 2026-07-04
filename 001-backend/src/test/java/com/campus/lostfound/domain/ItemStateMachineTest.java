package com.campus.lostfound.domain;

import com.campus.lostfound.lostfound.item.ItemStateMachine;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ItemStateMachineTest {

    @Test
    void allowsNormalFoundItemClaimFlow() {
        assertDoesNotThrow(() -> ItemStateMachine.assertTransition("DRAFT", "PENDING_REVIEW"));
        assertDoesNotThrow(() -> ItemStateMachine.assertTransition("PENDING_REVIEW", "PUBLISHED"));
        assertDoesNotThrow(() -> ItemStateMachine.assertTransition("PUBLISHED", "CLAIM_REVIEWING"));
        assertDoesNotThrow(() -> ItemStateMachine.assertTransition("CLAIM_REVIEWING", "HANDOVER_PENDING"));
        assertDoesNotThrow(() -> ItemStateMachine.assertTransition("HANDOVER_PENDING", "COMPLETED"));
        assertDoesNotThrow(() -> ItemStateMachine.assertTransition("COMPLETED", "ARCHIVED"));
    }

    @Test
    void rejectsSkippingReviewAndHandover() {
        assertThrows(IllegalArgumentException.class, () -> ItemStateMachine.assertTransition("DRAFT", "PUBLISHED"));
        assertThrows(IllegalArgumentException.class, () -> ItemStateMachine.assertTransition("PUBLISHED", "COMPLETED"));
    }
}
