package dev.rinchan.raiseresurrectionascend;

public final class FriendFeedingPolicy {
    public static final int MAXIMUM_VANILLA_FOOD_LEVEL = 20;

    private FriendFeedingPolicy() {
    }

    public static boolean canReceiveFood(int foodLevel) {
        return foodLevel >= 0 && foodLevel <= MAXIMUM_VANILLA_FOOD_LEVEL;
    }

    public static int exclusiveFoodLevelUpperBound() {
        return MAXIMUM_VANILLA_FOOD_LEVEL + 1;
    }
}
