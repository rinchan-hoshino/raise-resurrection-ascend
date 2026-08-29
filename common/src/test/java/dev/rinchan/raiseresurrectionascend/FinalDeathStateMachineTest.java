package dev.rinchan.raiseresurrectionascend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class FinalDeathStateMachineTest {
    @Test
    void dispatchCannotReenterAndCompletesOnlyFromTheDirectNativeResult() {
        FinalDeathStateMachine machine = new FinalDeathStateMachine();
        assertTrue(machine.requestFinalDeath());
        assertTrue(machine.beginDispatch());
        assertFalse(machine.requestFinalDeath());
        assertFalse(machine.beginDispatch());
        assertEquals(
            FinalDeathStateMachine.Outcome.REMAIN_DOWNED,
            machine.completeDispatch(false, false)
        );

        assertTrue(machine.requestFinalDeath());
        assertTrue(machine.beginDispatch());
        assertEquals(
            FinalDeathStateMachine.Outcome.TOTEM_TRIGGERED,
            machine.completeDispatch(true, false)
        );

        assertTrue(machine.requestFinalDeath());
        assertTrue(machine.beginDispatch());
        assertEquals(
            FinalDeathStateMachine.Outcome.FINAL_DEATH,
            machine.completeDispatch(false, true)
        );
    }

    @Test
    void canceledDeathDoesNotBecomeFreeRecoveryAndCanRetryOnTheNextTick() {
        FinalDeathStateMachine machine = new FinalDeathStateMachine();
        assertTrue(machine.requestFinalDeath());
        assertTrue(machine.beginDispatch());
        assertEquals(
            FinalDeathStateMachine.Outcome.REMAIN_DOWNED,
            machine.completeDispatch(false, false)
        );
        assertEquals(FinalDeathStateMachine.Phase.DOWNED, machine.phase());
        assertTrue(machine.requestFinalDeath());
    }
}
