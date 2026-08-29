package dev.rinchan.raiseresurrectionascend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class DownedRecoveryPolicyTest {
    @Test
    void thresholdTracksMaxHealthButIsCappedAtTwenty() {
        assertEquals(8.0F, DownedRecoveryPolicy.threshold(8.0F));
        assertEquals(20.0F, DownedRecoveryPolicy.threshold(40.0F));
        assertFalse(DownedRecoveryPolicy.canRecover(7.99F, 8.0F));
        assertTrue(DownedRecoveryPolicy.canRecover(8.0F, 8.0F));
        assertTrue(DownedRecoveryPolicy.requiresFullHealth(8.0F));

        assertFalse(DownedRecoveryPolicy.canRecover(19.0F, 40.0F));
        assertTrue(DownedRecoveryPolicy.canRecover(19.01F, 40.0F));
        assertTrue(DownedRecoveryPolicy.canRecover(20.0F, 40.0F));
        assertFalse(DownedRecoveryPolicy.requiresFullHealth(40.0F));
    }
}
