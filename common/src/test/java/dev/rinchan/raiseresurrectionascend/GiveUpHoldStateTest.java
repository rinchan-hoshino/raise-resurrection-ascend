package dev.rinchan.raiseresurrectionascend;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class GiveUpHoldStateTest {
    @Test
    void requiresFortyConsecutiveServerTicks() {
        GiveUpHoldState hold = new GiveUpHoldState(40);
        hold.setPressed(true);

        for (int tick = 0; tick < 39; tick++) {
            assertFalse(hold.tick());
        }
        assertTrue(hold.tick());
    }

    @Test
    void releaseResetsProgress() {
        GiveUpHoldState hold = new GiveUpHoldState(40);
        hold.setPressed(true);
        for (int tick = 0; tick < 25; tick++) {
            assertFalse(hold.tick());
        }

        hold.setPressed(false);
        assertFalse(hold.tick());
        hold.setPressed(true);
        for (int tick = 0; tick < 39; tick++) {
            assertFalse(hold.tick());
        }
        assertTrue(hold.tick());
    }

    @Test
    void completionLatchesUntilRelease() {
        GiveUpHoldState hold = new GiveUpHoldState(2);
        hold.setPressed(true);
        assertFalse(hold.tick());
        assertTrue(hold.tick());
        assertFalse(hold.tick());

        hold.setPressed(false);
        hold.setPressed(true);
        assertFalse(hold.tick());
        assertTrue(hold.tick());
    }
}
