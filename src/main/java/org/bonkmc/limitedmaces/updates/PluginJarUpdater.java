package org.bonkmc.limitedmaces.updates;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

final class PluginJarUpdater {
    private final JavaPlugin plugin;
    private final ModrinthClient modrinthClient;
    private final PluginJarValidator jarValidator;
    private final PluginJarLocator jarLocator;

    PluginJarUpdater(JavaPlugin plugin, ModrinthClient modrinthClient) {
        this.plugin = plugin;
        this.modrinthClient = modrinthClient;
        this.jarValidator = new PluginJarValidator();
        this.jarLocator = new PluginJarLocator(jarValidator);
    }

    Path install(ModrinthRelease release) throws IOException, InterruptedException {
        Path installedJar = jarLocator.locate(plugin);
        Path pluginsDirectory = jarLocator.pluginsDirectory(plugin);
        Path downloadedJar = Files.createTempFile(pluginsDirectory, ".limitedmaces-update-", ".tmp");

        try {
            modrinthClient.download(release, downloadedJar);
            jarValidator.validate(
                    downloadedJar,
                    plugin.getName(),
                    plugin.getDescription().getMain(),
                    release.versionNumber()
            );
            replaceInstalledJar(downloadedJar, installedJar);
            return installedJar;
        } finally {
            Files.deleteIfExists(downloadedJar);
        }
    }

    private void replaceInstalledJar(Path downloadedJar, Path installedJar) throws IOException {
        try {
            Files.move(
                    downloadedJar,
                    installedJar,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING
            );
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(downloadedJar, installedJar, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
