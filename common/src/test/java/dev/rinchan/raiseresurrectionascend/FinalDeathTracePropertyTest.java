package dev.rinchan.raiseresurrectionascend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

final class FinalDeathTracePropertyTest {
    @Test
    void everyTraceUpToSixOperationsMatchesTheNonReentrantReferenceMachine() {
        verifyAllTraces(ProductionMachine::new, 6);
    }

    @Test
    void negativeFixtureRejectsDispatchThatCanBeginTwice() {
        List<Operation> minimalReentry = List.of(Operation.REQUEST, Operation.BEGIN, Operation.BEGIN);
        assertThrows(AssertionError.class, () -> verifyTrace(ReentrantDispatchMachine::new, minimalReentry));
    }

    private static void verifyAllTraces(Supplier<TraceMachine> factory, int maxDepth) {
        enumerate(factory, maxDepth, new ArrayList<>());
    }

    private static void enumerate(Supplier<TraceMachine> factory, int remainingDepth, List<Operation> trace) {
        verifyTrace(factory, trace);
        if (remainingDepth == 0) {
            return;
        }
        for (Operation operation : Operation.values()) {
            trace.add(operation);
            enumerate(factory, remainingDepth - 1, trace);
            trace.remove(trace.size() - 1);
        }
    }

    private static void verifyTrace(Supplier<TraceMachine> factory, List<Operation> trace) {
        TraceMachine actual = factory.get();
        ReferenceMachine expected = new ReferenceMachine();
        for (int index = 0; index < trace.size(); index++) {
            Operation operation = trace.get(index);
            Observation expectedObservation = operation.apply(expected);
            Observation actualObservation = operation.apply(actual);
            int step = index;
            assertEquals(
                expectedObservation,
                actualObservation,
                () -> "trace=" + List.copyOf(trace) + ", step=" + step
            );
        }
    }

    private interface TraceMachine {
        FinalDeathStateMachine.Phase phase();

        boolean requestFinalDeath();

        boolean beginDispatch();

        FinalDeathStateMachine.Outcome completeDispatch(boolean totemTriggered, boolean deathCompleted);
    }

    private static final class ProductionMachine implements TraceMachine {
        private final FinalDeathStateMachine delegate = new FinalDeathStateMachine();

        @Override
        public FinalDeathStateMachine.Phase phase() {
            return delegate.phase();
        }

        @Override
        public boolean requestFinalDeath() {
            return delegate.requestFinalDeath();
        }

        @Override
        public boolean beginDispatch() {
            return delegate.beginDispatch();
        }

        @Override
        public FinalDeathStateMachine.Outcome completeDispatch(boolean totemTriggered, boolean deathCompleted) {
            return delegate.completeDispatch(totemTriggered, deathCompleted);
        }
    }

    private static class ReferenceMachine implements TraceMachine {
        protected FinalDeathStateMachine.Phase phase = FinalDeathStateMachine.Phase.DOWNED;

        @Override
        public FinalDeathStateMachine.Phase phase() {
            return phase;
        }

        @Override
        public boolean requestFinalDeath() {
            if (phase != FinalDeathStateMachine.Phase.DOWNED) {
                return false;
            }
            phase = FinalDeathStateMachine.Phase.REQUESTED;
            return true;
        }

        @Override
        public boolean beginDispatch() {
            if (phase != FinalDeathStateMachine.Phase.REQUESTED) {
                return false;
            }
            phase = FinalDeathStateMachine.Phase.DISPATCHING;
            return true;
        }

        @Override
        public FinalDeathStateMachine.Outcome completeDispatch(boolean totemTriggered, boolean deathCompleted) {
            if (phase != FinalDeathStateMachine.Phase.DISPATCHING) {
                return FinalDeathStateMachine.Outcome.REMAIN_DOWNED;
            }
            phase = FinalDeathStateMachine.Phase.DOWNED;
            if (deathCompleted) {
                return FinalDeathStateMachine.Outcome.FINAL_DEATH;
            }
            return totemTriggered
                ? FinalDeathStateMachine.Outcome.TOTEM_TRIGGERED
                : FinalDeathStateMachine.Outcome.REMAIN_DOWNED;
        }
    }

    private static final class ReentrantDispatchMachine extends ReferenceMachine {
        @Override
        public boolean beginDispatch() {
            if (phase != FinalDeathStateMachine.Phase.REQUESTED
                && phase != FinalDeathStateMachine.Phase.DISPATCHING) {
                return false;
            }
            phase = FinalDeathStateMachine.Phase.DISPATCHING;
            return true;
        }
    }

    private record Observation(Object result, FinalDeathStateMachine.Phase phase) {
    }

    private enum Operation {
        REQUEST {
            @Override
            Observation apply(TraceMachine machine) {
                return new Observation(machine.requestFinalDeath(), machine.phase());
            }
        },
        BEGIN {
            @Override
            Observation apply(TraceMachine machine) {
                return new Observation(machine.beginDispatch(), machine.phase());
            }
        },
        COMPLETE_NONE {
            @Override
            Observation apply(TraceMachine machine) {
                return complete(machine, false, false);
            }
        },
        COMPLETE_TOTEM {
            @Override
            Observation apply(TraceMachine machine) {
                return complete(machine, true, false);
            }
        },
        COMPLETE_DEATH {
            @Override
            Observation apply(TraceMachine machine) {
                return complete(machine, false, true);
            }
        },
        COMPLETE_BOTH {
            @Override
            Observation apply(TraceMachine machine) {
                return complete(machine, true, true);
            }
        };

        abstract Observation apply(TraceMachine machine);

        static Observation complete(TraceMachine machine, boolean totemTriggered, boolean deathCompleted) {
            return new Observation(machine.completeDispatch(totemTriggered, deathCompleted), machine.phase());
        }
    }
}
