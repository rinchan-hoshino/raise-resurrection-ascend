package dev.rinchan.raiseresurrectionascend;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class DowningSourceContractTest {
    @Test
    void finalDeathUsesReconstructedOriginalSourceInsteadOfDirectDie() throws Exception {
        String lifecycle = source("RaiseResurrectionAscend.java");
        String snapshot = source("DowningCauseSnapshot.java");
        assertTrue(lifecycle.contains("DamageSource downingSource = state.cause.reconstruct(player)"));
        assertTrue(lifecycle.contains("player.hurt(downingSource, Float.MAX_VALUE)"));
        assertFalse(lifecycle.contains("player.die("));
        assertFalse(lifecycle.contains("genericKill()"));
        assertTrue(snapshot.contains("RecordedMessageDamageSource"));
        assertTrue(snapshot.contains("return recordedMessage.copy()"));
        assertFalse(snapshot.contains("genericKill()"));
    }

    private static String source(String file) throws Exception {
        return Files.readString(root().resolve("common/src/main/java/dev/rinchan/raiseresurrectionascend").resolve(file));
    }

    private static Path root() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null && !Files.isDirectory(current.resolve("common/src/main"))) current = current.getParent();
        if (current == null) throw new IllegalStateException("Cannot locate project root");
        return current;
    }
}
