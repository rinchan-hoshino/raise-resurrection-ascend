package dev.rinchan.raiseresurrectionascend;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import org.junit.jupiter.api.Test;

final class PublishedVersionContractTest {
    @Test
    void publishedVersionIsOneZeroTwo() throws Exception {
        Properties properties = new Properties();
        try (var reader = Files.newBufferedReader(root().resolve("gradle.properties"))) {
            properties.load(reader);
        }
        assertEquals("1.0.2", properties.getProperty("mod_version"));
    }

    private static Path root() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null && !Files.exists(current.resolve("gradle.properties"))) {
            current = current.getParent();
        }
        if (current == null) {
            throw new IllegalStateException("Cannot locate project root");
        }
        return current;
    }
}
