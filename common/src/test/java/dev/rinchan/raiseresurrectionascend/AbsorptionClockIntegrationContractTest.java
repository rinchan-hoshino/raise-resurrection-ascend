package dev.rinchan.raiseresurrectionascend;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class AbsorptionClockIntegrationContractTest {
    @Test
    void downedLifecycleUsesLiveAbsorptionInsteadOfAnIndependentDeadline() throws IOException {
        String lifecycle = readSource(
            "common/src/main/java/dev/rinchan/raiseresurrectionascend/RaiseResurrectionAscend.java"
        );
        assertTrue(lifecycle.contains("DownedAbsorptionPolicy.initialAbsorption(player.getAbsorptionAmount())"));
        assertTrue(lifecycle.contains("DownedAbsorptionPolicy.drain(player.getAbsorptionAmount())"));
        assertTrue(lifecycle.contains("player.setAbsorptionAmount(result.absorption())"));
        assertFalse(lifecycle.contains("DOWNED_UNTIL_TICK"));
        assertFalse(lifecycle.contains("downedDurationTicks"));

        String packet = readSource(
            "common/src/main/java/dev/rinchan/raiseresurrectionascend/RaiseResurrectionAscendStatePacket.java"
        );
        assertFalse(packet.contains("remainingTicks"));
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
        throw new IOException("Unable to locate " + relative);
    }
}
