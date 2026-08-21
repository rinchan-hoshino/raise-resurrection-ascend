package dev.rinchan.raiseresurrectionascend;

public final class DownedDamagePolicy {
    private DownedDamagePolicy() {
    }

    public static boolean finishesDownedState(float health, float absorption, float damage) {
        return damage > 0.0F && damage >= health + Math.max(0.0F, absorption);
    }
}
