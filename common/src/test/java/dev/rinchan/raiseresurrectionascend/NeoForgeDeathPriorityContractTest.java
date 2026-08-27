package dev.rinchan.raiseresurrectionascend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class NeoForgeDeathPriorityContractTest {
    @Test
    void downingPrecedesLowestPriorityFinalDeathObservers() throws Exception {
        String adapter = Files.readString(root().resolve(
            "neoforge/src/main/java/dev/rinchan/raiseresurrectionascend/neoforge/RaiseResurrectionAscendNeoForge.java"
        ));

        assertTrue(adapter.contains(
            "addListener(EventPriority.LOW, this::onLivingDeath)"
        ));
        assertFalse(adapter.contains(
            "addListener(EventPriority.LOWEST, this::onLivingDeath)"
        ));
        assertEquals(3, occurrences(adapter, "addListener(EventPriority.LOWEST"));
        assertTrue(adapter.contains(
            "addListener(EventPriority.LOWEST, this::onLivingDamagePre)"
        ));
        assertTrue(adapter.contains(
            "addListener(EventPriority.LOWEST, this::onLivingDamagePost)"
        ));
        assertTrue(adapter.contains(
            "addListener(EventPriority.LOWEST, this::onLivingUseTotem)"
        ));
    }

    private static int occurrences(String text, String needle) {
        int count = 0;
        int offset = 0;
        while ((offset = text.indexOf(needle, offset)) >= 0) {
            count++;
            offset += needle.length();
        }
        return count;
    }

    private static Path root() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null && !Files.exists(current.resolve("gradle.properties"))) {
            current = current.getParent();
        }
        if (current == null) {
            throw new IllegalStateException("Cannot locate project root");
        }
        return current;
    }
}
