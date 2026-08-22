package dev.rinchan.raiseresurrectionascend;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.rinchan.raiseresurrectionascend.client.GiveUpHoldProgress;
import org.junit.jupiter.api.Test;

final class GiveUpHoldProgressTest {
    @Test
    void progressIsAlwaysBoundedByTheConfiguredHoldDuration() {
        assertEquals(0.0F, GiveUpHoldProgress.ratio(0, 40));
        assertEquals(0.5F, GiveUpHoldProgress.ratio(20, 40));
        assertEquals(1.0F, GiveUpHoldProgress.ratio(40, 40));
        assertEquals(1.0F, GiveUpHoldProgress.ratio(80, 40));
    }

    @Test
    void invalidHoldDurationsCannotHideOrOverflowTheIndicator() {
        assertEquals(0.0F, GiveUpHoldProgress.ratio(-1, 40));
        assertEquals(1.0F, GiveUpHoldProgress.ratio(1, 0));
    }
}
