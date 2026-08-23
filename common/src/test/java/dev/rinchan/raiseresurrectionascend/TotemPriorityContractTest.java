package dev.rinchan.raiseresurrectionascend;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class TotemPriorityContractTest {
    @Test
    void initialLethalDamageWaitsForVanillaTotemProtectionBeforeEnteringDowned() throws IOException {
        String adapter = readSource(
                "common/src/main/java/dev/rinchan/raiseresurrectionascend/neoforge/RaiseResurrectionAscendNeoForge.java");
        String damageHandler = methodBody(adapter, "private void onLivingDamagePre", "private void onLivingDeath");

        assertTrue(adapter.contains("EventPriority.LOW, this::onLivingDeath"));
        assertTrue(adapter.contains("private void onLivingDeath(LivingDeathEvent event)"));
        assertTrue(adapter.contains("event.setCanceled(true)"));
        assertTrue(adapter.contains("RaiseResurrectionAscend.enterDowned(player, event.getSource())"));
        assertFalse(damageHandler.contains("shouldEnterDowned"));
        assertFalse(damageHandler.contains("enterDowned"));
    }

    @Test
    void lethalDamageWhileDownedStillReachesTheVanillaDeathProtectionPipeline() throws IOException {
        String adapter = readSource(
                "common/src/main/java/dev/rinchan/raiseresurrectionascend/neoforge/RaiseResurrectionAscendNeoForge.java");
        String lifecycle = readSource(
                "common/src/main/java/dev/rinchan/raiseresurrectionascend/RaiseResurrectionAscend.java");

        assertTrue(adapter.contains("DownedDamagePolicy.damageForDeathProtection"));
        assertTrue(adapter.contains("RaiseResurrectionAscend.awaitDamageResolution(player)"));
        assertTrue(lifecycle.contains("public static boolean resolveDamageProtection(ServerPlayer player)"));
        assertTrue(lifecycle.contains("AWAITING_DAMAGE_RESOLUTION.remove(player.getUUID())"));
        assertTrue(lifecycle.contains("!player.isAlive()"));
        assertTrue(lifecycle.contains("clearDownedState(player)"));
    }

    private static String methodBody(String source, String startMarker, String endMarker) {
        int start = source.indexOf(startMarker);
        int end = source.indexOf(endMarker, start);
        assertTrue(start >= 0 && end > start);
        return source.substring(start, end);
    }

    private static String readSource(String relative) throws IOException {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve(relative);
            if (Files.isRegularFile(candidate)) {
                return Files.readString(candidate);
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Cannot locate " + relative);
    }
}
