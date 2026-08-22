package dev.rinchan.raiseresurrectionascend.client;

public final class GiveUpHoldProgress {
    private GiveUpHoldProgress() {
    }

    public static float ratio(int heldTicks, int requiredTicks) {
        if (requiredTicks <= 0) {
            return heldTicks > 0 ? 1.0F : 0.0F;
        }
        return Math.clamp((float) heldTicks / requiredTicks, 0.0F, 1.0F);
    }
}
