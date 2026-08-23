package dev.rinchan.raiseresurrectionascend;

public final class DownedRecoveryPolicy {
    private DownedRecoveryPolicy() {
    }

    public static boolean canRecover(float health, float maxHealth) {
        return health >= Math.min(maxHealth, 20.0F);
    }
}
