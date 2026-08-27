package dev.rinchan.raiseresurrectionascend;

public final class DownedRecoveryPolicy {
    private DownedRecoveryPolicy() {
    }

    public static float threshold(float maxHealth) {
        return Math.min(maxHealth, 20.0F);
    }

    public static boolean canRecover(float health, float maxHealth) {
        return health >= threshold(maxHealth);
    }
}
