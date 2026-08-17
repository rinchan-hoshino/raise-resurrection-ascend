package dev.rinchan.raiseresurrectionascend;

public final class RecoveryFeedingPolicy {
    private RecoveryFeedingPolicy() {
    }

    public static Result resolve(
        boolean targetDowned,
        ItemKind itemKind,
        int itemCount,
        boolean creative
    ) {
        boolean accepted = targetDowned && itemKind != ItemKind.UNSUPPORTED && itemCount > 0;
        if (!accepted) {
            return new Result(false, itemCount, false);
        }
        return new Result(
            true,
            creative ? itemCount : itemCount - 1,
            !creative && itemKind == ItemKind.RECOVERY_POTION
        );
    }

    public enum ItemKind {
        UNSUPPORTED,
        RECOVERY_POTION,
        GOLDEN_APPLE,
        ENCHANTED_GOLDEN_APPLE
    }

    public record Result(boolean accepted, int remainingItems, boolean returnBottle) {
    }
}
