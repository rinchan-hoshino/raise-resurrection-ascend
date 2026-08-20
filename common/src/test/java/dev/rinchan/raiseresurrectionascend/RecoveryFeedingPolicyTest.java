package dev.rinchan.raiseresurrectionascend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class RecoveryFeedingPolicyTest {
    @Test
    void ordinaryAppleHealsExactlyOneHeartAndConsumesOneItem() {
        var result = RecoveryFeedingPolicy.resolve(
                true,
                RecoveryFeedingPolicy.ItemKind.APPLE,
                4,
                false);

        assertTrue(result.accepted());
        assertEquals(2.0F, result.healing());
        assertEquals(3, result.remainingCount());
    }

    @Test
    void creativeFeedingDoesNotConsumeTheApple() {
        var result = RecoveryFeedingPolicy.resolve(
                true,
                RecoveryFeedingPolicy.ItemKind.APPLE,
                1,
                true);

        assertTrue(result.accepted());
        assertEquals(2.0F, result.healing());
        assertEquals(1, result.remainingCount());
    }

    @Test
    void unsupportedItemsStandingTargetsAndEmptyStacksAreRejected() {
        var unsupported = RecoveryFeedingPolicy.resolve(
                true,
                RecoveryFeedingPolicy.ItemKind.UNSUPPORTED,
                2,
                false);

        assertFalse(unsupported.accepted());
        assertEquals(0.0F, unsupported.healing());
        assertEquals(2, unsupported.remainingCount());
        assertFalse(RecoveryFeedingPolicy.resolve(
                false,
                RecoveryFeedingPolicy.ItemKind.APPLE,
                1,
                false).accepted());
        assertFalse(RecoveryFeedingPolicy.resolve(
                true,
                RecoveryFeedingPolicy.ItemKind.APPLE,
                0,
                false).accepted());
    }
}
