package dev.rinchan.raiseresurrectionascend;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DownedRecoveryPolicyTest {
    @Test
    void onlyFullHealthCompletesRecovery() {
        assertFalse(DownedRecoveryPolicy.isFullyHealed(1.0F, 20.0F));
        assertFalse(DownedRecoveryPolicy.isFullyHealed(19.99F, 20.0F));
        assertTrue(DownedRecoveryPolicy.isFullyHealed(20.0F, 20.0F));
        assertTrue(DownedRecoveryPolicy.isFullyHealed(24.0F, 20.0F));
    }
}
