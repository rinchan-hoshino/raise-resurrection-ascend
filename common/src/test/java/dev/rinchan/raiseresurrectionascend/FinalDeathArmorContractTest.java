package dev.rinchan.raiseresurrectionascend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class FinalDeathArmorContractTest {
    @Test
    void finalDeathSourceAddsOnlyArmorBypassToTheRecordedCause() throws Exception {
        String snapshot = Files.readString(
            root().resolve("common/src/main/java/dev/rinchan/raiseresurrectionascend/DowningCauseSnapshot.java")
        );

        assertTrue(snapshot.contains("return FinalDeathDamageSource.create("));
        assertTrue(snapshot.contains("static final class FinalDeathDamageSource extends DamageSource"));
        assertTrue(snapshot.contains("public boolean is(TagKey<DamageType> tag)"));
        assertTrue(snapshot.contains("DamageTypeTags.BYPASSES_ARMOR.equals(tag) || super.is(tag)"));
        assertEquals(1, occurrences(snapshot, "DamageTypeTags.BYPASSES_ARMOR"));
        assertFalse(snapshot.contains("DamageTypeTags.BYPASSES_SHIELD"));
        assertFalse(snapshot.contains("DamageTypeTags.BYPASSES_INVULNERABILITY"));
        assertFalse(snapshot.contains("DamageTypeTags.BYPASSES_EFFECTS"));
        assertFalse(snapshot.contains("DamageTypeTags.BYPASSES_RESISTANCE"));
        assertFalse(snapshot.contains("DamageTypeTags.BYPASSES_ENCHANTMENTS"));
    }

    private static int occurrences(String text, String needle) {
        return (text.length() - text.replace(needle, "").length()) / needle.length();
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
