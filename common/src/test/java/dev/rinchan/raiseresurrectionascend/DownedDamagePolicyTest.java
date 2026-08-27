package dev.rinchan.raiseresurrectionascend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class DownedDamagePolicyTest {
    @Test
    void damageBelowRemainingAbsorptionDoesNotFinishDownedState() {
        assertFalse(DownedDamagePolicy.finishesDownedState(10.0F, 4.0F));
        assertFalse(DownedDamagePolicy.finishesDownedState(0.0F, 0.0F));
        assertEquals(4.0F, DownedDamagePolicy.damageBeforeOriginalDispatch(10.0F, 4.0F));
    }

    @Test
    void finishingDamageStopsAtAbsorptionBoundaryForOriginalCauseDispatch() {
        assertTrue(DownedDamagePolicy.finishesDownedState(10.0F, 10.0F));
        assertEquals(10.0F, DownedDamagePolicy.damageBeforeOriginalDispatch(10.0F, 100.0F));
        assertEquals(0.0F, DownedDamagePolicy.damageBeforeOriginalDispatch(0.0F, 2.0F));
    }
}
