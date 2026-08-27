package dev.rinchan.raiseresurrectionascend;

/** Server-owned hold timer. Client packets may change only the pressed state. */
public final class GiveUpHoldState {
    public static final int REQUIRED_TICKS = 40;

    private boolean pressed;
    private int heldTicks;

    public void setPressed(boolean pressed) {
        this.pressed = pressed;
        if (!pressed) {
            heldTicks = 0;
        }
    }

    public boolean tickAndIsComplete() {
        if (!pressed) {
            return false;
        }
        if (heldTicks < REQUIRED_TICKS) {
            heldTicks++;
        }
        return heldTicks >= REQUIRED_TICKS;
    }

    public boolean pressed() {
        return pressed;
    }

    public int heldTicks() {
        return heldTicks;
    }
}
