package dev.rinchan.raiseresurrectionascend;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class AppleHealingContractTest {
    @Test
    void healingBelongsToTheAppleFoodComponent() throws Exception {
        String neoForge = readSource("common/src/main/java/dev/rinchan/raiseresurrectionascend/neoforge/RaiseResurrectionAscendNeoForge.java");
        String feeder = readSource("common/src/main/java/dev/rinchan/raiseresurrectionascend/mixin/PlayerFeederMixin.java");
        String recovery = readSource("common/src/main/java/dev/rinchan/raiseresurrectionascend/RaiseResurrectionAscend.java");
        assertTrue(neoForge.contains("event.modify(Items.APPLE"));
        assertTrue(neoForge.contains("new FoodProperties.PossibleEffect"));
        assertTrue(neoForge.contains("new MobEffectInstance(ONE_HEART_FOOD_EFFECT"));
        assertFalse(neoForge.contains("LivingEntityUseItemEvent.Finish"));
        assertFalse(feeder.contains("AppleHealing"));
        assertFalse(recovery.contains("tryFeedRecoveryItem"));
    }

    private static String readSource(String relativePath) throws Exception {
        Path current = Path.of("").toAbsolutePath();
        for (int depth = 0; depth < 4 && current != null; depth++, current = current.getParent()) {
            Path candidate = current.resolve(relativePath);
            if (Files.exists(candidate)) {
                return Files.readString(candidate);
            }
        }
        throw new IllegalStateException("Unable to resolve " + relativePath);
    }
}
