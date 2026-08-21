package dev.rinchan.raiseresurrectionascend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DownedAbsorptionPolicyTest {
    @Test
    void generatedAbsorptionLastsExactlyOneMinute() {
        float absorption = DownedAbsorptionPolicy.initialAbsorption(0.0F);
        assertEquals(20.0F, absorption);

        for (int tick = 0; tick < 1199; tick++) {
            var result = DownedAbsorptionPolicy.drain(absorption);
            assertFalse(result.expired());
            absorption = result.absorption();
        }
        var finalTick = DownedAbsorptionPolicy.drain(absorption);
        assertTrue(finalTick.expired());
        assertEquals(0.0F, finalTick.absorption());
    }

    @Test
    void addedAbsorptionNaturallyExtendsTheClock() {
        float absorption = DownedAbsorptionPolicy.initialAbsorption(0.0F) + 4.0F;
        for (int tick = 0; tick < 1200; tick++) {
            absorption = DownedAbsorptionPolicy.drain(absorption).absorption();
        }
        assertEquals(4.0F, absorption, 0.001F);
    }

    @Test
    void removedAbsorptionNaturallyShortensTheClock() {
        float absorption = DownedAbsorptionPolicy.initialAbsorption(0.0F) - 10.0F;
        for (int tick = 0; tick < 599; tick++) {
            var result = DownedAbsorptionPolicy.drain(absorption);
            assertFalse(result.expired());
            absorption = result.absorption();
        }
        assertTrue(DownedAbsorptionPolicy.drain(absorption).expired());
    }

    @Test
    void enteringDownedNeverReducesExistingAbsorption() {
        assertEquals(32.0F, DownedAbsorptionPolicy.initialAbsorption(32.0F));
    }
}
