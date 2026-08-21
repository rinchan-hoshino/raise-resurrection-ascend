package dev.rinchan.raiseresurrectionascend;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class DownedDamagePolicyTest {
    @Test
    void ordinaryHitsConsumeRemainingAbsorption() {
        assertFalse(DownedDamagePolicy.finishesDownedState(20.0F, 7.0F));
        assertFalse(DownedDamagePolicy.finishesDownedState(13.0F, 7.0F));
    }

    @Test
    void aHitThatClearsOrExceedsRemainingAbsorptionFinishesTheDownedState() {
        assertTrue(DownedDamagePolicy.finishesDownedState(6.0F, 7.0F));
        assertTrue(DownedDamagePolicy.finishesDownedState(20.0F, 20.0F));
        assertTrue(DownedDamagePolicy.finishesDownedState(20.0F, 21.0F));
    }

    @Test
    void zeroOrNegativeDamageDoesNotFinishTheDownedState() {
        assertFalse(DownedDamagePolicy.finishesDownedState(20.0F, 0.0F));
        assertFalse(DownedDamagePolicy.finishesDownedState(20.0F, -1.0F));
    }
}
