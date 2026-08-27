package dev.rinchan.raiseresurrectionascend;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class DownedPresentationContractTest {
    @Test
    void crawlAndServerOwnedGiveUpPresentationAreMaintainedOnBothSides() throws Exception {
        Path root = root();
        String lifecycle = Files.readString(root.resolve("common/src/main/java/dev/rinchan/raiseresurrectionascend/RaiseResurrectionAscend.java"));
        String client = Files.readString(root.resolve("common/src/main/java/dev/rinchan/raiseresurrectionascend/client/RaiseResurrectionAscendClient.java"));
        String english = Files.readString(root.resolve("common/src/main/resources/assets/raise_resurrection_ascend/lang/en_us.json"));
        assertTrue(lifecycle.contains("setPose(Pose.SWIMMING)"));
        assertTrue(client.contains("setPose(Pose.SWIMMING)"));
        assertFalse(lifecycle.contains("setForcedPose"));
        assertFalse(client.contains("setForcedPose"));
        assertTrue(client.contains("packet.recoveryThreshold()"));
        assertTrue(client.contains("formatHealth(recoveryThreshold)"));
        assertTrue(client.contains("GiveUpHoldState.REQUIRED_TICKS"));
        assertTrue(client.contains("hud.raise_resurrection_ascend.give_up"));
        assertTrue(client.contains("GuiGraphicsExtractor"));
        assertTrue(client.contains("graphics.centeredText"));
        assertTrue(english.contains("Hold %s for %s seconds to give up"));
    }

    private static Path root() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null && !Files.isDirectory(current.resolve("common/src/main"))) current = current.getParent();
        if (current == null) throw new IllegalStateException("Cannot locate project root");
        return current;
    }
}
