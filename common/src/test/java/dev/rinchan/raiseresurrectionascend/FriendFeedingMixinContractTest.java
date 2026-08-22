package dev.rinchan.raiseresurrectionascend;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class FriendFeedingMixinContractTest {
    @Test
    void fullHungerCompatibilityChangesOnlyTheOriginalModsHungerGates() throws IOException {
        String mixins = readSource("common/src/main/resources/raise_resurrection_ascend.mixins.json");
        assertTrue(mixins.contains("PlayerFeederMixin"));
        assertTrue(mixins.contains("SpecialFoodHandlerMixin"));

        String ordinary = readSource(
            "common/src/main/java/dev/rinchan/raiseresurrectionascend/mixin/PlayerFeederMixin.java"
        );
        assertTrue(ordinary.contains("th.in.tamkungz.letyourfriendeating.logic.PlayerFeeder"));
        assertTrue(ordinary.contains("method = \"tryFeedPlayer\""));
        assertTrue(ordinary.contains("FriendDrinkFeeder.tryFeedDrink(feeder, recipient, hand)"));
        assertTrue(ordinary.contains("method = \"validateItemAndPlayer\""));
        assertTrue(ordinary.contains("FriendFeedingPolicy.exclusiveFoodLevelUpperBound()"));

        String special = readSource(
            "common/src/main/java/dev/rinchan/raiseresurrectionascend/mixin/SpecialFoodHandlerMixin.java"
        );
        assertTrue(special.contains("th.in.tamkungz.letyourfriendeating.logic.SpecialFoodHandler"));
        assertTrue(special.contains("\"handleEternalFood\", \"handleStew\""));
        assertTrue(special.contains("FoodData;needsFood()Z"));
        assertTrue(special.contains("FriendFeedingPolicy.canReceiveFood(foodData.getFoodLevel())"));

        String metadata = readSource("neoforge/src/main/templates/META-INF/neoforge.mods.toml");
        assertTrue(metadata.contains("config=\"raise_resurrection_ascend.mixins.json\""));
        assertTrue(metadata.contains("modId=\"letyourfriendeating\""));
        assertTrue(metadata.contains("type=\"optional\""));
        assertTrue(metadata.contains("versionRange=\"[1.1.4,1.2)\""));

        String drinkFeeder = readSource(
            "common/src/main/java/dev/rinchan/raiseresurrectionascend/FriendDrinkFeeder.java"
        );
        assertTrue(drinkFeeder.contains("UseAnim.DRINK"));
        assertTrue(drinkFeeder.contains("CooldownManager.canFeed"));
        assertTrue(drinkFeeder.contains("CooldownManager.recordFeed"));
        assertTrue(drinkFeeder.contains("FeedStatsSavedData.get"));
        assertTrue(drinkFeeder.contains("finishUsingItem(level, recipient)"));
        assertTrue(drinkFeeder.contains("held.is(Items.MILK_BUCKET)"));
    }

    private static String readSource(String relative) throws IOException {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve(relative);
            if (Files.isRegularFile(candidate)) {
                return Files.readString(candidate);
            }
            current = current.getParent();
        }
        throw new IOException("Unable to locate " + relative);
    }
}
