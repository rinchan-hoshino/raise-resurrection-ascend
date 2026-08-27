package dev.rinchan.raiseresurrectionascend;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class DownedPresentationContractTest {
    @Test
    void crawlIsMaintainedOnBothSidesAndHudShowsRecoveryAndGiveUpProgress() throws Exception {
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
        assertTrue(client.contains("give_up"));
        assertTrue(client.contains("GIVE_UP_HOLD_TICKS"));
        assertTrue(english.contains("Hold G to give up"));
    }

    private static Path root() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null && !Files.isDirectory(current.resolve("common/src/main"))) current = current.getParent();
        if (current == null) throw new IllegalStateException("Cannot locate project root");
        return current;
    }
}
