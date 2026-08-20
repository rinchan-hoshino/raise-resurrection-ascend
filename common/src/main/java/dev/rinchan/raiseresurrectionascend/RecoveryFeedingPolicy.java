package dev.rinchan.raiseresurrectionascend;

public final class RecoveryFeedingPolicy {
    public enum ItemKind {
        APPLE,
        UNSUPPORTED
    }

    public record Result(boolean accepted, float healing, int remainingCount) {
    }

    private RecoveryFeedingPolicy() {
    }

    public static Result resolve(boolean downed, ItemKind itemKind, int stackCount, boolean creative) {
        if (!downed || itemKind != ItemKind.APPLE || stackCount <= 0) {
            return new Result(false, 0.0F, stackCount);
        }
        return new Result(true, 2.0F, creative ? stackCount : Math.max(0, stackCount - 1));
    }
}
