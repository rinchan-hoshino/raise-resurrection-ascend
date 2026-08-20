package dev.rinchan.raiseresurrectionascend;

public final class DownedDamagePolicy {
    private DownedDamagePolicy() {
    }

    public static boolean finishesDownedState(float health, float damage) {
        return damage > 0.0F && damage >= health;
    }
}
