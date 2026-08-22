package dev.rinchan.raiseresurrectionascend;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class FriendDrinkPolicyTest {
    @Test
    void interceptsDrinkAnimationUnlessTheFeedingModAlreadyOwnsTheItem() {
        assertTrue(FriendDrinkPolicy.shouldHandle(true, false));
        assertFalse(FriendDrinkPolicy.shouldHandle(true, true));
    }

    @Test
    void leavesFoodAndOrdinaryItemUseAlone() {
        assertFalse(FriendDrinkPolicy.shouldHandle(false, false));
    }

    @Test
    void preservesCreativeOrReusableDrinksAndConsumesSurvivalContainers() {
        assertTrue(FriendDrinkPolicy.preservesHeldItem(true, false));
        assertTrue(FriendDrinkPolicy.preservesHeldItem(false, true));
        assertFalse(FriendDrinkPolicy.preservesHeldItem(false, false));
    }
}
