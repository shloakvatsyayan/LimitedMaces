package org.bonkmc.limitedmaces.updates;

import org.bonkmc.limitedmaces.LimitedMaces;
import org.bukkit.Bukkit;

import java.io.IOException;
import java.nio.file.Path;

public final class UpdateService {
    private final String currentVersion;
    private final String gameVersion;
    private final ModrinthClient modrinthClient;
    private final PluginJarUpdater jarUpdater;

    public UpdateService(LimitedMaces plugin) {
        this.currentVersion = plugin.getDescription().getVersion();
        this.gameVersion = new GameVersionResolver().resolve(Bukkit.getBukkitVersion());
        this.modrinthClient = new ModrinthClient(currentVersion);
        this.jarUpdater = new PluginJarUpdater(plugin, modrinthClient);
    }

    public UpdateStatus check() throws IOException, InterruptedException {
        ModrinthRelease latestRelease = modrinthClient.fetchLatest(gameVersion).orElse(null);
        return new UpdateStatus(currentVersion, gameVersion, latestRelease);
    }

    public String gameVersion() {
        return gameVersion;
    }

    public Path installLatest(UpdateStatus updateStatus) throws IOException, InterruptedException {
        if (!updateStatus.isUpdateAvailable()) {
            throw new IllegalStateException("No newer compatible version is available");
        }
        return jarUpdater.install(updateStatus.latestRelease());
    }
}
