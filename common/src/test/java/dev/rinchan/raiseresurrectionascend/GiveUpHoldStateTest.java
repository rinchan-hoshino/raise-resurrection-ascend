package dev.rinchan.raiseresurrectionascend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class GiveUpHoldStateTest {
    @Test
    void completionRequiresFortyServerTicksAfterPress() {
        GiveUpHoldState state = new GiveUpHoldState();
        state.setPressed(true);
        for (int tick = 1; tick < GiveUpHoldState.REQUIRED_TICKS; tick++) {
            assertFalse(state.tickAndIsComplete());
        }
        assertTrue(state.tickAndIsComplete());
        assertEquals(40, state.heldTicks());
    }

    @Test
    void releaseResetsAllProgress() {
        GiveUpHoldState state = new GiveUpHoldState();
        state.setPressed(true);
        for (int tick = 0; tick < 25; tick++) {
            state.tickAndIsComplete();
        }
        state.setPressed(false);
        assertFalse(state.pressed());
        assertEquals(0, state.heldTicks());
        assertFalse(state.tickAndIsComplete());
    }

    @Test
    void completedHoldStaysCompleteUntilRelease() {
        GiveUpHoldState state = new GiveUpHoldState();
        state.setPressed(true);
        for (int tick = 0; tick < GiveUpHoldState.REQUIRED_TICKS; tick++) {
            state.tickAndIsComplete();
        }
        assertTrue(state.tickAndIsComplete());
        assertEquals(GiveUpHoldState.REQUIRED_TICKS, state.heldTicks());
    }
}
