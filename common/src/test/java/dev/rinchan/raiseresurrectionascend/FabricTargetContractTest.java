package dev.rinchan.raiseresurrectionascend;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class FabricTargetContractTest {
    @Test
    void commonCoreHasNoNeoForgeLoaderImports() throws Exception {
        Path common = root().resolve("common/src/main/java");
        try (var paths = Files.walk(common)) {
            for (Path path : paths.filter(file -> file.toString().endsWith(".java")).toList()) {
                assertFalse(Files.readString(path).contains("import net.neoforged."), path.toString());
            }
        }
    }

    @Test
    void fabricOwnsRequiredLifecycleNetworkingAndInteractionCallbacks() throws Exception {
        String server = fabricSource("fabric/RaiseResurrectionAscendFabric.java");
        String client = fabricSource("client/fabric/RaiseResurrectionAscendFabricClient.java");
        assertTrue(server.contains("PayloadTypeRegistry.clientboundPlay()"));
        assertTrue(server.contains("ServerPlayNetworking::send"));
        assertTrue(server.contains("ServerLivingEntityEvents.ALLOW_DEATH"));
        assertTrue(server.contains("ServerTickEvents.END_SERVER_TICK"));
        assertTrue(server.contains("ServerPlayConnectionEvents.JOIN"));
        assertTrue(server.contains("ServerPlayConnectionEvents.DISCONNECT"));
        assertTrue(server.contains("UseEntityCallback.EVENT"));
        assertTrue(client.contains("ClientPlayNetworking.registerGlobalReceiver"));
        assertTrue(client.contains("ClientTickEvents.END_CLIENT_TICK"));
        assertTrue(client.contains("HudElementRegistry.addLast"));
        assertTrue(client.contains("ClientPlayConnectionEvents.INIT"));
        assertTrue(client.contains("ClientPlayConnectionEvents.DISCONNECT"));
    }

    @Test
    void fabricPersistsStructuredCauseAndGuardsFollowUpTotemChecks() throws Exception {
        String persistence = fabricSource("mixin/fabric/ServerPlayerPersistenceMixin.java");
        String guard = fabricSource("mixin/fabric/LivingEntityTotemGuardMixin.java");
        String totem = commonSource("TotemProtection.java");
        assertTrue(persistence.contains("addAdditionalSaveData"));
        assertTrue(persistence.contains("readAdditionalSaveData"));
        assertTrue(persistence.contains("DownedStatePersistence.STORAGE_KEY"));
        assertTrue(guard.contains("@At(\"HEAD\")"));
        assertTrue(guard.contains("permitsNativeTotemCheck"));
        assertTrue(guard.contains("observeNativeTotemTrigger"));
        assertTrue(totem.contains("withSyntheticTotemRescue"));
        assertTrue(totem.contains("finally"));
        assertTrue(totem.contains("held.shrink(1)"));
        assertTrue(totem.contains("instabuild"));
    }

    @Test
    void fabricMetadataIsBothSidesAndHasOnlyRequiredPlatformDependencies() throws Exception {
        String metadata = Files.readString(root().resolve("fabric/src/main/resources/fabric.mod.json"));
        String build = Files.readString(root().resolve("fabric/build.gradle"));
        assertTrue(metadata.contains("\"environment\": \"*\""));
        assertTrue(metadata.contains("\"main\""));
        assertTrue(metadata.contains("\"client\""));
        assertTrue(metadata.contains("\"fabric-api\""));
        assertFalse(metadata.contains("food"));
        assertFalse(metadata.contains("feed"));
        assertTrue(build.contains("com.mojang:minecraft:${rootProject.minecraft_version}"));
        assertFalse(build.contains("loom.officialMojangMappings()"));
        assertFalse(build.contains("let-your-friend-eating"));
    }

    private static String commonSource(String relative) throws Exception {
        return Files.readString(root().resolve("common/src/main/java/dev/rinchan/raiseresurrectionascend").resolve(relative));
    }

    private static String fabricSource(String relative) throws Exception {
        return Files.readString(root().resolve("fabric/src/main/java/dev/rinchan/raiseresurrectionascend").resolve(relative));
    }

    private static Path root() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null && !Files.isDirectory(current.resolve("common/src/main"))) {
            current = current.getParent();
        }
        if (current == null) {
            throw new IllegalStateException("Cannot locate project root");
        }
        return current;
    }
}
