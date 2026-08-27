package dev.rinchan.raiseresurrectionascend;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

final class RraOneZeroContractTest {
    @Test
    void finalDeathReentersNativeDamageAndTotemPipelineWithPersistedCause() throws Exception {
        String lifecycle = source("RaiseResurrectionAscend.java");
        String cause = source("DowningCauseSnapshot.java");
        String adapter = loaderSource("neoforge", "dev/rinchan/raiseresurrectionascend/neoforge/RaiseResurrectionAscendNeoForge.java");
        String totem = source("TotemProtection.java");

        assertTrue(lifecycle.contains("player.hurt(downingSource, FINAL_DEATH_DAMAGE)"));
        assertTrue(lifecycle.contains("private static final float FINAL_DEATH_DAMAGE = 1_000_000.0F;"));
        assertFalse(lifecycle.contains("Float.MAX_VALUE"));
        assertFalse(lifecycle.contains("player.die("));
        assertTrue(cause.contains("damage_type"));
        assertTrue(cause.contains("direct_entity"));
        assertTrue(cause.contains("causing_entity"));
        assertTrue(cause.contains("source_position"));
        assertTrue(cause.contains("death_message"));
        assertFalse(lifecycle.contains("genericKill()"));
        assertTrue(adapter.contains("LivingUseTotemEvent"));
        assertTrue(adapter.contains("PlayerInteractEvent.EntityInteract"));
        assertTrue(adapter.contains("Items.TOTEM_OF_UNDYING"));
        assertTrue(totem.contains("raiseResurrectionAscend$invokeTotemDeathProtection"));
    }

    @Test
    void networkIsRequiredVersionedAndGiveUpPayloadCarriesOnlyPressedState() throws Exception {
        String adapter = loaderSource("neoforge", "dev/rinchan/raiseresurrectionascend/neoforge/RaiseResurrectionAscendNeoForge.java");
        String neoForgeClient = loaderSource("neoforge", "dev/rinchan/raiseresurrectionascend/client/neoforge/RaiseResurrectionAscendNeoForgeClient.java");
        String statePacket = source("RaiseResurrectionAscendStatePacket.java");
        String giveUpPacket = source("RaiseResurrectionAscendGiveUpPacket.java");
        String client = source("client/RaiseResurrectionAscendClient.java");

        assertTrue(adapter.contains("event.registrar(\"1.0.1\")"));
        assertFalse(adapter.contains(".optional()"));
        assertTrue(adapter.contains("playToServer"));
        assertFalse(adapter.contains("exceptionally("));
        assertTrue(adapter.contains("LOGGER.error"));
        assertTrue(statePacket.contains("float recoveryThreshold"));
        assertTrue(giveUpPacket.contains("boolean pressed"));
        assertFalse(giveUpPacket.contains("clientTick"));
        assertTrue(client.contains("packet.recoveryThreshold()"));
        assertTrue(neoForgeClient.contains("ClientPlayerNetworkEvent.LoggingOut"));
        assertTrue(client.contains("give_up"));
    }

    @Test
    void removedProductionScopeAndDependenciesAreAbsent() throws Exception {
        Path root = root();
        List<String> removed = List.of(
            "common/src/main/java/dev/rinchan/raiseresurrectionascend/FriendDrinkFeeder.java",
            "common/src/main/java/dev/rinchan/raiseresurrectionascend/FriendDrinkPolicy.java",
            "common/src/main/java/dev/rinchan/raiseresurrectionascend/FriendFeedingPolicy.java",
            "common/src/main/java/dev/rinchan/raiseresurrectionascend/RaiseResurrectionAscendConfig.java",
            "common/src/main/java/dev/rinchan/raiseresurrectionascend/client/ScreenshotClientHarness.java",
            "common/src/main/java/dev/rinchan/raiseresurrectionascend/neoforge/ScreenshotServerHarness.java",
            "common/src/main/java/dev/rinchan/raiseresurrectionascend/gametest/TotemPriorityGameTests.java",
            "common/src/main/java/dev/rinchan/raiseresurrectionascend/mixin/PlayerFeederMixin.java",
            "common/src/main/java/dev/rinchan/raiseresurrectionascend/mixin/SpecialFoodHandlerMixin.java",
            "common/src/main/resources/data/raise_resurrection_ascend/structure/empty.nbt"
        );
        for (String relative : removed) {
            assertFalse(Files.exists(root.resolve(relative)), relative);
        }

        String build = Files.readString(root.resolve("neoforge/build.gradle"));
        String metadata = Files.readString(root.resolve("neoforge/src/main/templates/META-INF/neoforge.mods.toml"));
        assertFalse(build.contains("let-your-friend-eating"));
        assertFalse(build.contains("gameTestServer"));
        assertFalse(metadata.contains("letyourfriendeating"));
        assertTrue(metadata.contains("[[mixins]]"));
        assertTrue(metadata.contains("raise_resurrection_ascend.mixins.json"));
    }

    private static String source(String relative) throws Exception {
        return Files.readString(root().resolve("common/src/main/java/dev/rinchan/raiseresurrectionascend").resolve(relative));
    }

    private static String loaderSource(String loader, String relative) throws Exception {
        return Files.readString(root().resolve(loader).resolve("src/main/java").resolve(relative));
    }

    private static Path root() {
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
