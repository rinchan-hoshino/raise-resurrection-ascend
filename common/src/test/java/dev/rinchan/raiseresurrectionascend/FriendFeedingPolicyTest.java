package dev.rinchan.raiseresurrectionascend;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FriendFeedingPolicyTest {
    @Test
    void everyVanillaHungerLevelCanReceiveFood() {
        assertTrue(FriendFeedingPolicy.canReceiveFood(0));
        assertTrue(FriendFeedingPolicy.canReceiveFood(19));
        assertTrue(FriendFeedingPolicy.canReceiveFood(20));
    }

    @Test
    void invalidFoodLevelsRemainRejected() {
        assertFalse(FriendFeedingPolicy.canReceiveFood(-1));
        assertFalse(FriendFeedingPolicy.canReceiveFood(21));
    }
}
