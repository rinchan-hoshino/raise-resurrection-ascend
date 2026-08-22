package dev.rinchan.raiseresurrectionascend;

public final class FriendDrinkPolicy {
    private FriendDrinkPolicy() {
    }

    public static boolean shouldHandle(boolean drinkAnimation, boolean handledByOriginalMod) {
        return drinkAnimation && !handledByOriginalMod;
    }

    public static boolean preservesHeldItem(boolean feederCreative, boolean unchangedRemainder) {
        return feederCreative || unchangedRemainder;
    }
}
