package dev.rinchan.raiseresurrectionascend;

final class GiveUpHoldState {
    private final int requiredTicks;
    private boolean pressed;
    private boolean completed;
    private int heldTicks;

    GiveUpHoldState(int requiredTicks) {
        if (requiredTicks <= 0) {
            throw new IllegalArgumentException("requiredTicks must be positive");
        }
        this.requiredTicks = requiredTicks;
    }

    void setPressed(boolean pressed) {
        this.pressed = pressed;
        if (!pressed) {
            heldTicks = 0;
            completed = false;
        }
    }

    boolean tick() {
        if (!pressed || completed) {
            return false;
        }
        heldTicks++;
        if (heldTicks < requiredTicks) {
            return false;
        }
        completed = true;
        return true;
    }
}
