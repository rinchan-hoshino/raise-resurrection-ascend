package dev.rinchan.raiseresurrectionascend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class FinalDeathStateMachineTest {
    @Test
    void dispatchCannotReenterAndClearsOnlyAfterObservedTotemOrDeath() {
        FinalDeathStateMachine machine = new FinalDeathStateMachine();
        assertTrue(machine.requestFinalDeath());
        assertTrue(machine.beginDispatch());
        assertFalse(machine.requestFinalDeath());
        assertFalse(machine.beginDispatch());
        assertEquals(
            FinalDeathStateMachine.Outcome.REMAIN_DOWNED,
            machine.completeDispatch(true)
        );

        assertTrue(machine.requestFinalDeath());
        assertTrue(machine.beginDispatch());
        machine.observeTotemTrigger();
        assertEquals(
            FinalDeathStateMachine.Outcome.TOTEM_TRIGGERED,
            machine.completeDispatch(true)
        );

        assertTrue(machine.requestFinalDeath());
        assertTrue(machine.beginDispatch());
        machine.observeFinalDeath();
        assertEquals(
            FinalDeathStateMachine.Outcome.FINAL_DEATH,
            machine.completeDispatch(false)
        );
    }

    @Test
    void canceledDispatchDoesNotBecomeFreeRecoveryAndCanRetryOnTheNextTick() {
        FinalDeathStateMachine machine = new FinalDeathStateMachine();
        assertTrue(machine.requestFinalDeath());
        assertTrue(machine.beginDispatch());
        machine.observeTotemTrigger();
        assertEquals(
            FinalDeathStateMachine.Outcome.REMAIN_DOWNED,
            machine.completeDispatch(false)
        );
        assertEquals(FinalDeathStateMachine.Phase.DOWNED, machine.phase());
        assertTrue(machine.requestFinalDeath());
    }
}
