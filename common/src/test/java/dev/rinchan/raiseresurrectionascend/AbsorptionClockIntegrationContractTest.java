package dev.rinchan.raiseresurrectionascend;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class AbsorptionClockIntegrationContractTest {
    @Test
    void lifecycleUsesOneAbsorptionClockForEntryTickAndPersistence() throws Exception {
        String source = Files.readString(sourcePath("RaiseResurrectionAscend.java"));
        assertTrue(source.contains("DownedAbsorptionPolicy.initialAbsorption"));
        assertTrue(source.contains("DownedAbsorptionPolicy.drain"));
        assertTrue(source.contains("remaining_absorption"));
        assertTrue(source.contains("state.finalDeath.requestFinalDeath()"));
    }

    private static Path sourcePath(String file) {
        return root().resolve("common/src/main/java/dev/rinchan/raiseresurrectionascend").resolve(file);
    }

    private static Path root() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null && !Files.isDirectory(current.resolve("common/src/main"))) {
            current = current.getParent();
        }
        if (current == null) throw new IllegalStateException("Cannot locate project root");
        return current;
    }
}
