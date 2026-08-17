package dev.rinchan.raiseresurrectionascend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class RecoveryFeedingPolicyTest {
    @Test
    void recoveryPotionIsConsumedAndReturnsABottle() {
        RecoveryFeedingPolicy.Result result = RecoveryFeedingPolicy.resolve(
            true,
            RecoveryFeedingPolicy.ItemKind.RECOVERY_POTION,
            2,
            false
        );

        assertTrue(result.accepted());
        assertEquals(1, result.remainingItems());
        assertTrue(result.returnBottle());
    }

    @Test
    void eitherGoldenAppleIsConsumedWithoutAContainer() {
        for (RecoveryFeedingPolicy.ItemKind kind : new RecoveryFeedingPolicy.ItemKind[] {
            RecoveryFeedingPolicy.ItemKind.GOLDEN_APPLE,
            RecoveryFeedingPolicy.ItemKind.ENCHANTED_GOLDEN_APPLE
        }) {
            RecoveryFeedingPolicy.Result result = RecoveryFeedingPolicy.resolve(true, kind, 1, false);
            assertTrue(result.accepted(), kind.name());
            assertEquals(0, result.remainingItems(), kind.name());
            assertFalse(result.returnBottle(), kind.name());
        }
    }

    @Test
    void creativeFeedingConsumesNothing() {
        RecoveryFeedingPolicy.Result result = RecoveryFeedingPolicy.resolve(
            true,
            RecoveryFeedingPolicy.ItemKind.ENCHANTED_GOLDEN_APPLE,
            1,
            true
        );

        assertTrue(result.accepted());
        assertEquals(1, result.remainingItems());
        assertFalse(result.returnBottle());
    }

    @Test
    void unsupportedItemsAndStandingTargetsAreRejected() {
        assertFalse(RecoveryFeedingPolicy.resolve(
            true,
            RecoveryFeedingPolicy.ItemKind.UNSUPPORTED,
            1,
            false
        ).accepted());
        assertFalse(RecoveryFeedingPolicy.resolve(
            false,
            RecoveryFeedingPolicy.ItemKind.GOLDEN_APPLE,
            1,
            false
        ).accepted());
        assertFalse(RecoveryFeedingPolicy.resolve(
            true,
            RecoveryFeedingPolicy.ItemKind.GOLDEN_APPLE,
            0,
            false
        ).accepted());
    }
}
