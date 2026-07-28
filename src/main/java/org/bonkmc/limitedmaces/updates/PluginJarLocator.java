package org.bonkmc.limitedmaces.updates;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

final class PluginJarLocator {
    private final PluginJarValidator jarValidator;

    PluginJarLocator(PluginJarValidator jarValidator) {
        this.jarValidator = jarValidator;
    }

    Path locate(JavaPlugin plugin) throws IOException {
        Path pluginsDirectory = pluginsDirectory(plugin);
        Set<Path> candidates = new LinkedHashSet<>();
        Path codeSource = codeSource(plugin);

        if (codeSource != null && codeSource.getParent() != null) {
            if (codeSource.getParent().equals(pluginsDirectory)) {
                candidates.add(codeSource);
            }
            candidates.add(pluginsDirectory.resolve(codeSource.getFileName()));
        }

        try (Stream<Path> pluginFiles = Files.list(pluginsDirectory)) {
            pluginFiles.filter(this::isJar)
                    .forEach(candidates::add);
        }

        List<Path> matches = new ArrayList<>();
        for (Path candidate : candidates) {
            if (!Files.isRegularFile(candidate)) {
                continue;
            }
            if (identifiesPlugin(plugin, candidate)) {
                matches.add(candidate);
            }
        }

        if (matches.size() != 1) {
            throw new IOException("Expected one installed LimitedMaces jar but found " + matches.size());
        }
        return matches.getFirst();
    }

    Path pluginsDirectory(JavaPlugin plugin) throws IOException {
        File pluginsFolder = plugin.getDataFolder().getParentFile();
        if (pluginsFolder == null) {
            throw new IOException("Unable to locate the server plugins directory");
        }

        Path pluginsDirectory = pluginsFolder.toPath().toAbsolutePath().normalize();
        if (!Files.isDirectory(pluginsDirectory)) {
            throw new IOException("Server plugins directory does not exist: " + pluginsDirectory);
        }
        return pluginsDirectory;
    }

    private Path codeSource(JavaPlugin plugin) throws IOException {
        try {
            return Path.of(plugin.getClass().getProtectionDomain().getCodeSource().getLocation().toURI())
                    .toAbsolutePath()
                    .normalize();
        } catch (URISyntaxException | RuntimeException exception) {
            throw new IOException("Unable to resolve the running plugin jar", exception);
        }
    }

    private boolean isJar(Path candidate) {
        return candidate.getFileName().toString().toLowerCase().endsWith(".jar");
    }

    private boolean identifiesPlugin(JavaPlugin plugin, Path candidate) {
        try {
            return jarValidator.identifies(candidate, plugin.getName(), plugin.getDescription().getMain());
        } catch (IOException exception) {
            plugin.getLogger().fine("Skipping unreadable plugin jar " + candidate + ": " + exception.getMessage());
            return false;
        }
    }
}
