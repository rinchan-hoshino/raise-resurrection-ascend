package dev.rinchan.raiseresurrectionascend;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class MissingEntityRendererGuardContractTest {
    @Test
    void client_guard_logs_the_exact_missing_entity_and_skips_only_that_entity() throws Exception {
        Path root = root();
        String source = Files.readString(root.resolve(
                "common/src/main/java/dev/rinchan/raiseresurrectionascend/mixin/MissingEntityRendererGuardMixin.java"));
        String mixins = Files.readString(root.resolve("common/src/main/resources/raise_resurrection_ascend.mixins.json"));

        assertTrue(source.contains("@Mixin(EntityRenderDispatcher.class)"));
        assertTrue(source.contains("method = \"shouldRender\""));
        assertTrue(source.contains("getRenderer(entity) == null"));
        assertTrue(source.contains("BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType())"));
        assertTrue(source.contains("entity.getClass().getName()"));
        assertTrue(source.contains("entity.getUUID()"));
        assertTrue(source.contains("cir.setReturnValue(false)"));
        assertTrue(mixins.contains("MissingEntityRendererGuardMixin"));
        assertTrue(mixins.contains("\"client\""));
        assertFalse(mixins.substring(mixins.indexOf("\"mixins\""), mixins.indexOf("\"client\"")).contains(
                "MissingEntityRendererGuardMixin"));
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
