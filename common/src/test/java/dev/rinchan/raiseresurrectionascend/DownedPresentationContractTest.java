package dev.rinchan.raiseresurrectionascend;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class DownedPresentationContractTest {
    @Test
    void crawlCauseAndHeartBasedRecoveryPresentationAreMaintainedOnBothSides() throws Exception {
        Path root = root();
        String lifecycle = Files.readString(root.resolve("common/src/main/java/dev/rinchan/raiseresurrectionascend/RaiseResurrectionAscend.java"));
        String packet = Files.readString(root.resolve("common/src/main/java/dev/rinchan/raiseresurrectionascend/RaiseResurrectionAscendStatePacket.java"));
        String client = Files.readString(root.resolve("common/src/main/java/dev/rinchan/raiseresurrectionascend/client/RaiseResurrectionAscendClient.java"));
        String english = Files.readString(root.resolve("common/src/main/resources/assets/raise_resurrection_ascend/lang/en_us.json"));
        String chinese = Files.readString(root.resolve("common/src/main/resources/assets/raise_resurrection_ascend/lang/zh_cn.json"));
        assertTrue(lifecycle.contains("setPose(Pose.SWIMMING)"));
        assertTrue(client.contains("setPose(Pose.SWIMMING)"));
        assertFalse(lifecycle.contains("setForcedPose"));
        assertFalse(client.contains("setForcedPose"));
        assertTrue(lifecycle.contains("state.cause.recordedDeathMessage(player)"));
        assertTrue(packet.contains("Component deathMessage"));
        assertTrue(packet.contains("ComponentSerialization.TRUSTED_STREAM_CODEC"));
        assertTrue(client.contains("packet.deathMessage()"));
        assertTrue(client.contains("hud.raise_resurrection_ascend.cause"));
        assertTrue(client.contains("hud.raise_resurrection_ascend.recovery_ten_hearts"));
        assertTrue(client.contains("hud.raise_resurrection_ascend.recovery_full_hearts"));
        assertFalse(client.contains("formatHealth(recoveryThreshold)"));
        assertTrue(client.contains("GIVE_UP_HOLD_TICKS"));
        assertTrue(client.contains("hud.raise_resurrection_ascend.give_up"));
        assertTrue(client.contains("GuiGraphicsExtractor"));
        assertTrue(client.contains("graphics.centeredText"));
        assertTrue(english.contains("Hold %s for %s seconds to give up"));
        assertTrue(english.contains("Fatal blow: %s"));
        assertTrue(english.contains("Refill ten hearts to stand back up"));
        assertTrue(english.contains("Refill every heart to stand back up"));
        assertTrue(chinese.contains("致命伤：%s"));
        assertTrue(chinese.contains("恢复至十颗心即可重新站起"));
        assertTrue(chinese.contains("回满所有心即可重新站起"));
    }

    private static Path root() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null && !Files.isDirectory(current.resolve("common/src/main"))) current = current.getParent();
        if (current == null) throw new IllegalStateException("Cannot locate project root");
        return current;
    }
}
