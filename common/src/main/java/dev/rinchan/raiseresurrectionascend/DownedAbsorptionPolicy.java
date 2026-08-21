package dev.rinchan.raiseresurrectionascend;

public final class DownedAbsorptionPolicy {
    public static final float INITIAL_ABSORPTION = 20.0F;
    public static final int BASE_DURATION_TICKS = 20 * 60;
    public static final float DRAIN_PER_TICK = INITIAL_ABSORPTION / BASE_DURATION_TICKS;
    private static final float FLOAT_TOLERANCE = 0.001F;

    private DownedAbsorptionPolicy() {
    }

    public static float initialAbsorption(float existingAbsorption) {
        return Math.max(INITIAL_ABSORPTION, existingAbsorption);
    }

    public static DrainResult drain(float absorption) {
        if (absorption <= DRAIN_PER_TICK + FLOAT_TOLERANCE) {
            return new DrainResult(0.0F, true);
        }
        return new DrainResult(absorption - DRAIN_PER_TICK, false);
    }

    public record DrainResult(float absorption, boolean expired) {
    }
}
