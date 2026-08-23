package dev.rinchan.raiseresurrectionascend;

public final class DownedAbsorptionPolicy {
    public static final int BASE_DURATION_TICKS = 20 * 60;
    private static final float FLOAT_TOLERANCE = 0.001F;

    private DownedAbsorptionPolicy() {
    }

    public static float initialAbsorption(float existingAbsorption, float maxHealth) {
        return Math.max(generatedAbsorption(maxHealth), existingAbsorption);
    }

    public static DrainResult drain(float absorption, float maxHealth) {
        float drainPerTick = generatedAbsorption(maxHealth) / BASE_DURATION_TICKS;
        if (absorption <= drainPerTick + FLOAT_TOLERANCE) {
            return new DrainResult(0.0F, true);
        }
        return new DrainResult(absorption - drainPerTick, false);
    }

    public static float generatedAbsorption(float maxHealth) {
        return Math.max(0.0F, maxHealth);
    }

    public record DrainResult(float absorption, boolean expired) {
    }
}
