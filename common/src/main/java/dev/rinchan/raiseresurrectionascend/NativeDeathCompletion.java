package dev.rinchan.raiseresurrectionascend;

/** Records whether ServerPlayer.die reached its uncancelled native tail. */
final class NativeDeathCompletion {
    private boolean completed;

    void reset() {
        completed = false;
    }

    void markCompleted() {
        completed = true;
    }

    boolean completed() {
        return completed;
    }
}
