package dev.rinchan.raiseresurrectionascend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class PotionFeedingPolicyTest {
    @Test
    void ordinaryHealingPotionIsConsumedAndReturnsABottle() {
        PotionFeedingPolicy.Result result = PotionFeedingPolicy.resolve(true, true, true, 2, false);

        assertTrue(result.accepted());
        assertEquals(1, result.remainingPotions());
        assertEquals(1, result.returnedBottles());
    }

    @Test
    void creativeFeedingConsumesNothingAndReturnsNoBottle() {
        PotionFeedingPolicy.Result result = PotionFeedingPolicy.resolve(true, true, true, 1, true);

        assertTrue(result.accepted());
        assertEquals(1, result.remainingPotions());
        assertEquals(0, result.returnedBottles());
    }

    @Test
    void nonDrinkableHarmfulOrStandingTargetsAreRejected() {
        assertFalse(PotionFeedingPolicy.resolve(true, false, true, 1, false).accepted());
        assertFalse(PotionFeedingPolicy.resolve(true, true, false, 1, false).accepted());
        assertFalse(PotionFeedingPolicy.resolve(false, true, true, 1, false).accepted());
    }
}
