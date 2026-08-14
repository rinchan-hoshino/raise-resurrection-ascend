package dev.rinchan.raiseresurrectionascend;

public final class PotionFeedingPolicy {
    private PotionFeedingPolicy() {
    }

    public static Result resolve(
        boolean targetDowned,
        boolean ordinaryDrinkablePotion,
        boolean hasRecoveryEffect,
        int potionCount,
        boolean creative
    ) {
        boolean accepted = targetDowned && ordinaryDrinkablePotion && hasRecoveryEffect && potionCount > 0;
        if (!accepted) {
            return new Result(false, potionCount, 0);
        }
        if (creative) {
            return new Result(true, potionCount, 0);
        }
        return new Result(true, potionCount - 1, 1);
    }

    public record Result(boolean accepted, int remainingPotions, int returnedBottles) {
    }
}
