package com.clinic.booking;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

/** Pure boundary tests for the 2-hour change cutoff (no wall clock, no Spring). */
class ChangeCutoffPolicyTest {

    private final ChangeCutoffPolicy policy = new ChangeCutoffPolicy(2);

    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-07-20T10:00:00Z");

    @Test
    void wellBeforeCutoff_isAllowed() {
        assertTrue(policy.allowsChangeAt(NOW.plusHours(5), NOW));
    }

    @Test
    void exactlyAtCutoff_isAllowed() {
        // Exactly 2h ahead is on the boundary and permitted.
        assertTrue(policy.allowsChangeAt(NOW.plusHours(2), NOW));
    }

    @Test
    void insideCutoffWindow_isRejected() {
        assertFalse(policy.allowsChangeAt(NOW.plusMinutes(90), NOW));
    }

    @Test
    void alreadyStarted_isRejected() {
        assertFalse(policy.allowsChangeAt(NOW.minusMinutes(1), NOW));
    }
}
