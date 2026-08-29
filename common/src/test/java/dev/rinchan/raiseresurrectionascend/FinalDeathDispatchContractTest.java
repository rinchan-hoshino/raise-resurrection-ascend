package dev.rinchan.raiseresurrectionascend;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class FinalDeathDispatchContractTest {
    @Test
    void finalDeathUsesNativeTotemThenDirectDeathWithoutSyntheticDamage() throws Exception {
        String lifecycle = source("RaiseResurrectionAscend.java");
        String snapshot = source("DowningCauseSnapshot.java");
        String accessor = source("mixin/LivingEntityDeathAccessor.java");
        String mixins = Files.readString(root().resolve("common/src/main/resources/raise_resurrection_ascend.mixins.json"));

        assertTrue(lifecycle.contains("player.setHealth(0.0F)"));
        assertTrue(lifecycle.contains("raiseResurrectionAscend$invokeTotemDeathProtection(downingSource)"));
        assertTrue(lifecycle.contains("player.die(downingSource)"));
        assertTrue(lifecycle.contains("raiseResurrectionAscend$isDead()"));
        assertTrue(lifecycle.contains("completeDispatch(totemTriggered, deathCompleted)"));
        assertFalse(lifecycle.contains("player.hurt("));
        assertFalse(lifecycle.contains("FINAL_DEATH_DAMAGE"));
        assertFalse(lifecycle.contains("Float.MAX_VALUE"));
        assertFalse(lifecycle.contains("invulnerableTime = 0"));

        assertTrue(snapshot.contains("RecordedMessageDamageSource"));
        assertFalse(snapshot.contains("FinalDeathDamageSource"));
        assertFalse(snapshot.contains("DamageTypeTags"));
        assertFalse(snapshot.contains("BYPASSES_ARMOR"));
        assertTrue(accessor.contains("@Accessor(\"dead\")"));
        assertTrue(mixins.contains("LivingEntityDeathAccessor"));
    }

    private static String source(String file) throws Exception {
        return Files.readString(root().resolve("common/src/main/java/dev/rinchan/raiseresurrectionascend").resolve(file));
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
