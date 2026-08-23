package dev.rinchan.raiseresurrectionascend;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DownedRecoveryPolicyTest {
    @Test
    void recoveryRequiresFullHealthOrTwentyHealthWhicheverIsLower() {
        assertFalse(DownedRecoveryPolicy.canRecover(1.0F, 20.0F));
        assertFalse(DownedRecoveryPolicy.canRecover(19.99F, 40.0F));
        assertTrue(DownedRecoveryPolicy.canRecover(20.0F, 40.0F));
        assertTrue(DownedRecoveryPolicy.canRecover(24.0F, 40.0F));
        assertFalse(DownedRecoveryPolicy.canRecover(15.99F, 16.0F));
        assertTrue(DownedRecoveryPolicy.canRecover(16.0F, 16.0F));
    }
}
