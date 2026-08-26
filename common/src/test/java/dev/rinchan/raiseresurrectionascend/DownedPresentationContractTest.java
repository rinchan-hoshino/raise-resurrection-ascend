package dev.rinchan.raiseresurrectionascend;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class DownedPresentationContractTest {
    @Test
    void downedEntryBroadcastsOneServerWideChatIncludingTheDownedPlayer() throws IOException {
        String lifecycle = Files.readString(source("RaiseResurrectionAscend.java"));
        assertTrue(lifecycle.contains("broadcastSystemMessage"));
        assertTrue(lifecycle.contains("message.raise_resurrection_ascend.downed_other"));
        assertFalse(lifecycle.contains("other.displayClientMessage"));
        assertFalse(lifecycle.contains("other != player"));
        assertFalse(lifecycle.contains("player.serverLevel().players()"));
    }

    @Test
    void hudShowsOnlyRecoveryGiveUpAndAnAlwaysRenderedProgressBar() throws IOException {
        String client = Files.readString(source("client/RaiseResurrectionAscendClient.java"));
        assertFalse(client.contains("hud.raise_resurrection_ascend.downed"));
        assertFalse(client.contains("giveUpHoldTicks.get() / 20"));
        assertTrue(client.contains("GiveUpHoldProgress.ratio"));
        assertTrue(client.contains("PROGRESS_BAR_WIDTH"));

        String lang = Files.readString(resource("zh_cn.json"));
        assertFalse(lang.contains("message.raise_resurrection_ascend.downed_self"));
        assertFalse(lang.contains("message.raise_resurrection_ascend.recovered"));
        assertFalse(lang.contains("hud.raise_resurrection_ascend.downed"));
        assertTrue(lang.contains("\"hud.raise_resurrection_ascend.recovery\": \"恢复至满血可以解除濒死状态\""));
        assertTrue(lang.contains("\"hud.raise_resurrection_ascend.give_up\": \"长按 %s 放弃\""));
        assertTrue(lang.contains("\"message.raise_resurrection_ascend.downed_other\": \"%s 倒地了。\""));
    }

    private static Path source(String relative) {
        return locateRoot().resolve("common/src/main/java/dev/rinchan/raiseresurrectionascend").resolve(relative);
    }

    private static Path resource(String filename) {
        return locateRoot().resolve("common/src/main/resources/assets/raise_resurrection_ascend/lang").resolve(filename);
    }

    private static Path locateRoot() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            if (Files.isDirectory(current.resolve("common/src/main"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Cannot locate project root");
    }
}
