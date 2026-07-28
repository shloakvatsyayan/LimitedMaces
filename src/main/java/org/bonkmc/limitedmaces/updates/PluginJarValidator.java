package org.bonkmc.limitedmaces.updates;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

final class PluginJarValidator {
    boolean identifies(Path jarPath, String expectedName, String expectedMainClass) throws IOException {
        YamlConfiguration pluginMetadata = readMetadata(jarPath);
        return expectedName.equals(pluginMetadata.getString("name"))
                && expectedMainClass.equals(pluginMetadata.getString("main"));
    }

    void validate(
            Path jarPath,
            String expectedName,
            String expectedMainClass,
            String expectedVersion
    ) throws IOException {
        YamlConfiguration pluginMetadata = readMetadata(jarPath);
        if (!expectedName.equals(pluginMetadata.getString("name"))) {
            throw new IOException("Downloaded jar has an unexpected plugin name");
        }
        if (!expectedMainClass.equals(pluginMetadata.getString("main"))) {
            throw new IOException("Downloaded jar has an unexpected main class");
        }
        if (!expectedVersion.equals(pluginMetadata.getString("version"))) {
            throw new IOException("Downloaded jar version does not match Modrinth");
        }
    }

    private YamlConfiguration readMetadata(Path jarPath) throws IOException {
        try (JarFile pluginJar = new JarFile(jarPath.toFile())) {
            JarEntry pluginEntry = pluginJar.getJarEntry("plugin.yml");
            if (pluginEntry == null) {
                throw new IOException("Plugin jar does not contain plugin.yml");
            }

            try (InputStreamReader metadataReader = new InputStreamReader(
                    pluginJar.getInputStream(pluginEntry),
                    StandardCharsets.UTF_8
            )) {
                return YamlConfiguration.loadConfiguration(metadataReader);
            }
        }
    }
}
