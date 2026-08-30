package dev.rinchan.raiseresurrectionascend;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class ServerPlayerDeathCompletionTest {
    @Test
    void nativeCompletionIsMarkedOnlyAtTheUncancelledServerPlayerDieTail() throws Exception {
        Path root = root();
        String lifecycle = Files.readString(root.resolve(
            "common/src/main/java/dev/rinchan/raiseresurrectionascend/RaiseResurrectionAscend.java"
        ));
        String mixin = Files.readString(root.resolve(
            "common/src/main/java/dev/rinchan/raiseresurrectionascend/mixin/ServerPlayerDeathCompletionMixin.java"
        ));
        String mixins = Files.readString(root.resolve(
            "common/src/main/resources/raise_resurrection_ascend.mixins.json"
        ));

        assertTrue(mixin.contains("@Mixin(ServerPlayer.class)"));
        assertTrue(mixin.contains("@Inject(method = \"die\", at = @At(\"TAIL\"))"));
        assertTrue(mixin.contains("RaiseResurrectionAscend.observeNativeDeathCompletion"));
        assertTrue(lifecycle.contains("state.nativeDeathCompletion.reset()"));
        assertTrue(lifecycle.contains("player.die(downingSource)"));
        assertTrue(lifecycle.contains("deathCompleted = state.nativeDeathCompletion.completed()"));
        assertFalse(lifecycle.contains("raiseResurrectionAscend$isDead()"));
        assertTrue(mixins.contains("ServerPlayerDeathCompletionMixin"));
        assertFalse(mixins.contains("LivingEntityDeathAccessor"));
    }

    @Test
    void completionMarkerResetsBetweenDispatchAttempts() {
        NativeDeathCompletion completion = new NativeDeathCompletion();
        assertFalse(completion.completed());
        completion.markCompleted();
        assertTrue(completion.completed());
        completion.reset();
        assertFalse(completion.completed());
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
