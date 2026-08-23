package dev.rinchan.raiseresurrectionascend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DownedAbsorptionPolicyTest {
    @Test
    void generatedAbsorptionEqualsMaximumHealthAndLastsExactlyOneMinute() {
        float absorption = DownedAbsorptionPolicy.initialAbsorption(0.0F, 40.0F);
        assertEquals(40.0F, absorption);

        for (int tick = 0; tick < 1199; tick++) {
            var result = DownedAbsorptionPolicy.drain(absorption, 40.0F);
            assertFalse(result.expired());
            absorption = result.absorption();
        }
        var finalTick = DownedAbsorptionPolicy.drain(absorption, 40.0F);
        assertTrue(finalTick.expired());
        assertEquals(0.0F, finalTick.absorption());
    }

    @Test
    void addedAbsorptionNaturallyExtendsTheClock() {
        float absorption = DownedAbsorptionPolicy.initialAbsorption(0.0F, 40.0F) + 4.0F;
        for (int tick = 0; tick < 1200; tick++) {
            absorption = DownedAbsorptionPolicy.drain(absorption, 40.0F).absorption();
        }
        assertEquals(4.0F, absorption, 0.001F);
    }

    @Test
    void removedAbsorptionNaturallyShortensTheClock() {
        float absorption = DownedAbsorptionPolicy.initialAbsorption(0.0F, 40.0F) - 20.0F;
        for (int tick = 0; tick < 599; tick++) {
            var result = DownedAbsorptionPolicy.drain(absorption, 40.0F);
            assertFalse(result.expired());
            absorption = result.absorption();
        }
        assertTrue(DownedAbsorptionPolicy.drain(absorption, 40.0F).expired());
    }

    @Test
    void enteringDownedNeverReducesExistingAbsorption() {
        assertEquals(48.0F, DownedAbsorptionPolicy.initialAbsorption(48.0F, 40.0F));
    }
}
