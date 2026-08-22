package dev.rinchan.raiseresurrectionascend;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class AppleHealingContractTest {
    @Test
    void normalConsumptionAndFriendFeedingShareTheAppleHealingOwner() throws Exception {
        String neoForge = readSource("common/src/main/java/dev/rinchan/raiseresurrectionascend/neoforge/RaiseResurrectionAscendNeoForge.java");
        String feeder = readSource("common/src/main/java/dev/rinchan/raiseresurrectionascend/mixin/PlayerFeederMixin.java");
        String recovery = readSource("common/src/main/java/dev/rinchan/raiseresurrectionascend/RaiseResurrectionAscend.java");
        assertTrue(neoForge.contains("LivingEntityUseItemEvent.Finish"));
        assertTrue(neoForge.contains("AppleHealing.apply(player, event.getItem())"));
        assertTrue(feeder.contains("AppleHealing.apply(recipient, foodStack)"));
        assertTrue(recovery.contains("AppleHealing.apply(target, itemStack)"));
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
