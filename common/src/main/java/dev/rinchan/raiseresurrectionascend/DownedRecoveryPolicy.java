package dev.rinchan.raiseresurrectionascend;

public final class DownedRecoveryPolicy {
    private DownedRecoveryPolicy() {
    }

    public static boolean isFullyHealed(float health, float maxHealth) {
        return health >= maxHealth;
    }
}
