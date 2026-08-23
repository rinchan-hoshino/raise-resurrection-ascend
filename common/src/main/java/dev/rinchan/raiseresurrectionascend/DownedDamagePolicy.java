package dev.rinchan.raiseresurrectionascend;

public final class DownedDamagePolicy {
    private DownedDamagePolicy() {
    }

    public static boolean finishesDownedState(float absorption, float damage) {
        return damage > 0.0F && damage >= Math.max(0.0F, absorption);
    }

    public static float damageForDeathProtection(float absorption, float health, float damage) {
        if (!finishesDownedState(absorption, damage)) {
            return damage;
        }
        return Math.max(damage, Math.max(0.0F, absorption) + Math.max(0.0F, health));
    }
}
