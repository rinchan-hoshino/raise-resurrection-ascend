package dev.rinchan.raiseresurrectionascend;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class DownedDamagePolicyTest {
    @Test
    void nonlethalDamageContinuesToReduceDownedHealth() {
        assertFalse(DownedDamagePolicy.finishesDownedState(8.0F, 3.0F));
    }

    @Test
    void lethalDamageFinishesTheDownedState() {
        assertTrue(DownedDamagePolicy.finishesDownedState(8.0F, 8.0F));
        assertTrue(DownedDamagePolicy.finishesDownedState(1.0F, 2.0F));
    }

    @Test
    void zeroOrNegativeDamageDoesNotFinishTheDownedState() {
        assertFalse(DownedDamagePolicy.finishesDownedState(1.0F, 0.0F));
        assertFalse(DownedDamagePolicy.finishesDownedState(1.0F, -1.0F));
    }
}
