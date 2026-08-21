package dev.rinchan.raiseresurrectionascend;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class DownedDamagePolicyTest {
    @Test
    void absorptionTakesDownedDamageBeforeHealth() {
        assertFalse(DownedDamagePolicy.finishesDownedState(1.0F, 20.0F, 7.0F));
        assertFalse(DownedDamagePolicy.finishesDownedState(1.0F, 13.0F, 7.0F));
    }

    @Test
    void damageThatExhaustsAbsorptionAndHealthFinishesTheDownedState() {
        assertTrue(DownedDamagePolicy.finishesDownedState(1.0F, 6.0F, 7.0F));
        assertFalse(DownedDamagePolicy.finishesDownedState(1.0F, 20.0F, 20.0F));
        assertTrue(DownedDamagePolicy.finishesDownedState(1.0F, 20.0F, 21.0F));
        assertTrue(DownedDamagePolicy.finishesDownedState(1.0F, 0.0F, 1.0F));
    }

    @Test
    void zeroOrNegativeDamageDoesNotFinishTheDownedState() {
        assertFalse(DownedDamagePolicy.finishesDownedState(1.0F, 20.0F, 0.0F));
        assertFalse(DownedDamagePolicy.finishesDownedState(1.0F, 20.0F, -1.0F));
    }
}
