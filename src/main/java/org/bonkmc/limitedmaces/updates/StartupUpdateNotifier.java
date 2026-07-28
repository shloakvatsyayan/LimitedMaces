package org.bonkmc.limitedmaces.updates;

import org.bonkmc.limitedmaces.LimitedMaces;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;

import java.io.IOException;

public final class StartupUpdateNotifier implements Listener {
    private final LimitedMaces plugin;
    private final UpdateService updateService;

    public StartupUpdateNotifier(LimitedMaces plugin, UpdateService updateService) {
        this.plugin = plugin;
        this.updateService = updateService;
    }

    @EventHandler
    public void onServerLoad(ServerLoadEvent event) {
        if (event.getType() != ServerLoadEvent.LoadType.STARTUP) {
            return;
        }

        plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin, this::checkForUpdate, 20L);
    }

    private void checkForUpdate() {
        try {
            UpdateStatus updateStatus = updateService.check();
            if (!updateStatus.isUpdateAvailable()) {
                return;
            }

            String latestVersion = updateStatus.latestVersion().orElseThrow();
            plugin.getLogger().warning("LimitedMaces is running version "
                    + updateStatus.currentVersion() + " and needs to update to version " + latestVersion + ".");
            plugin.getLogger().warning("New versions often contain bug fixes and new features.");
            plugin.getLogger().warning("Update with /limitedmaces update or get the latest version from "
                    + "https://modrinth.com/plugin/limitedmaces.");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            plugin.getLogger().warning("LimitedMaces update check was interrupted.");
        } catch (IOException | RuntimeException exception) {
            plugin.getLogger().warning("Unable to check Modrinth for updates: " + exception.getMessage());
        }
    }
}
