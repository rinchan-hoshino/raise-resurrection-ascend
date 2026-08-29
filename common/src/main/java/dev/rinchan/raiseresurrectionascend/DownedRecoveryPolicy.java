package dev.rinchan.raiseresurrectionascend;

public final class DownedRecoveryPolicy {
    private static final float TEN_HEARTS = 20.0F;

    private DownedRecoveryPolicy() {
    }

    public static float threshold(float maxHealth) {
        return Math.min(maxHealth, TEN_HEARTS);
    }

    public static boolean requiresFullHealth(float recoveryThreshold) {
        return recoveryThreshold < TEN_HEARTS;
    }

    public static boolean canRecover(float health, float maxHealth) {
        float recoveryThreshold = threshold(maxHealth);
        if (requiresFullHealth(recoveryThreshold)) {
            return health >= recoveryThreshold;
        }
        return Math.ceil(health) >= TEN_HEARTS;
    }
}
