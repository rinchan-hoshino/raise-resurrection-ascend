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
    private boolean totemObserved;
    private boolean deathObserved;

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
        totemObserved = false;
        deathObserved = false;
        return true;
    }

    void observeTotemTrigger() {
        if (phase == Phase.DISPATCHING) {
            totemObserved = true;
        }
    }

    void observeFinalDeath() {
        if (phase == Phase.DISPATCHING) {
            deathObserved = true;
        }
    }

    Outcome completeDispatch(boolean playerAlive) {
        if (phase != Phase.DISPATCHING) {
            return Outcome.REMAIN_DOWNED;
        }
        phase = Phase.DOWNED;
        if (deathObserved) {
            return Outcome.FINAL_DEATH;
        }
        if (totemObserved && playerAlive) {
            return Outcome.TOTEM_TRIGGERED;
        }
        return Outcome.REMAIN_DOWNED;
    }
}
