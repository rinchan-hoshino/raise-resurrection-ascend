package dev.rinchan.raiseresurrectionascend;

public final class DownedDamagePolicy {
    private DownedDamagePolicy() {
    }

    public static boolean finishesDownedState(float absorption, float damage) {
        return damage > 0.0F && damage >= Math.max(0.0F, absorption);
    }
}
