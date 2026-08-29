package dev.rinchan.raiseresurrectionascend;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class TotemPriorityContractTest {
    @Test
    void bothFinalDeathAndFeederDelegateToNativeTotemActivation() throws Exception {
        String adapter = Files.readString(root().resolve("neoforge/src/main/java/dev/rinchan/raiseresurrectionascend/neoforge/RaiseResurrectionAscendNeoForge.java"));
        String lifecycle = source("RaiseResurrectionAscend.java");
        String totem = source("TotemProtection.java");
        String invoker = source("mixin/LivingEntityTotemInvoker.java");
        assertFalse(adapter.contains("LivingUseTotemEvent"));
        assertTrue(lifecycle.contains("raiseResurrectionAscend$invokeTotemDeathProtection(downingSource)"));
        assertTrue(totem.contains("raiseResurrectionAscend$invokeTotemDeathProtection"));
        assertTrue(totem.contains("finally"));
        assertTrue(invoker.contains("@Invoker(\"checkTotemDeathProtection\")"));
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
