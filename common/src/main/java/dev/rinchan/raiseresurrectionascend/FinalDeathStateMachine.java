package dev.rinchan.raiseresurrectionascend;

/** A synchronous, non-recursive final-death dispatch state machine. */
final class FinalDeathStateMachine {
    enum Phase {
        DOWNED,
        REQUESTED,
        DISPATCHING
    }

    enum Outcome {
        REMAIN_DOWNED,
        TOTEM_TRIGGERED,
        FINAL_DEATH
    }

    private Phase phase = Phase.DOWNED;

    Phase phase() {
        return phase;
    }

    boolean requestFinalDeath() {
        if (phase != Phase.DOWNED) {
            return false;
        }
        phase = Phase.REQUESTED;
        return true;
    }

    boolean beginDispatch() {
        if (phase != Phase.REQUESTED) {
            return false;
        }
        phase = Phase.DISPATCHING;
        return true;
    }

    Outcome completeDispatch(boolean totemTriggered, boolean deathCompleted) {
        if (phase != Phase.DISPATCHING) {
            return Outcome.REMAIN_DOWNED;
        }
        phase = Phase.DOWNED;
        if (deathCompleted) {
            return Outcome.FINAL_DEATH;
        }
        if (totemTriggered) {
            return Outcome.TOTEM_TRIGGERED;
        }
        return Outcome.REMAIN_DOWNED;
    }
}
