package org.bonkmc.limitedmaces.updates;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PluginJarValidatorTest {
    private final PluginJarValidator jarValidator = new PluginJarValidator();

    @TempDir
    Path testDirectory;

    @Test
    void acceptsJarWithExpectedPluginMetadata() throws IOException {
        Path pluginJar = createPluginJar("LimitedMaces", "org.example.LimitedMaces", "1.2.1");

        assertTrue(jarValidator.identifies(pluginJar, "LimitedMaces", "org.example.LimitedMaces"));
        assertDoesNotThrow(() -> jarValidator.validate(
                pluginJar,
                "LimitedMaces",
                "org.example.LimitedMaces",
                "1.2.1"
        ));
    }

    @Test
    void rejectsJarWhoseVersionDoesNotMatchRelease() throws IOException {
        Path pluginJar = createPluginJar("LimitedMaces", "org.example.LimitedMaces", "2.0.0");

        assertThrows(
                IOException.class,
                () -> jarValidator.validate(
                        pluginJar,
                        "LimitedMaces",
                        "org.example.LimitedMaces",
                        "1.2.1"
                )
        );
    }

    private Path createPluginJar(String pluginName, String mainClass, String pluginVersion) throws IOException {
        Path pluginJar = testDirectory.resolve(pluginName + "-" + pluginVersion + ".jar");
        String pluginMetadata = """
                name: %s
                main: %s
                version: %s
                """.formatted(pluginName, mainClass, pluginVersion);

        try (JarOutputStream jarOutput = new JarOutputStream(Files.newOutputStream(pluginJar))) {
            jarOutput.putNextEntry(new JarEntry("plugin.yml"));
            jarOutput.write(pluginMetadata.getBytes(StandardCharsets.UTF_8));
            jarOutput.closeEntry();
        }
        return pluginJar;
    }
}
