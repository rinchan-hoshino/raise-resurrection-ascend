package dev.rinchan.raiseresurrectionascend;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class DowningSourceContractTest {
    @Test
    void finalDeathUsesTheDamageSourceThatEnteredTheDownedState() throws IOException {
        String source = Files.readString(findSource());

        assertTrue(source.contains("enterDowned(ServerPlayer player, DamageSource damageSource)"));
        assertTrue(source.contains("DOWNING_SOURCES.put(player.getUUID(), damageSource)"));
        assertTrue(source.contains("player.getCombatTracker().recordDamage(downingSource"));
        assertTrue(source.contains("player.die(downingSource)"));
    }

    @Test
    void damageFinishDefersDeathOutsideTheLivingDamageCallStack() throws IOException {
        String source = Files.readString(findSource());
        int finishStart = source.indexOf("public static void finishDownedFromDamage");
        int dieStart = source.indexOf("private static void dieNow", finishStart);
        String finishBody = source.substring(finishStart, dieStart);

        assertTrue(finishBody.contains("PENDING_FINAL_DEATH.add"));
        assertTrue(!finishBody.contains("dieNow(player)"));
        assertTrue(source.contains("if (PENDING_FINAL_DEATH.remove(player.getUUID()))"));
    }

    private static Path findSource() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve(
                    "common/src/main/java/dev/rinchan/raiseresurrectionascend/RaiseResurrectionAscend.java");
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Cannot locate RaiseResurrectionAscend.java");
    }
}
