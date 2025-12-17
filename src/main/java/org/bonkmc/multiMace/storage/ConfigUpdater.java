package org.bonkmc.multiMace.storage;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Handles version-based configuration updates.
 * Supports upgrading configs from any version to any version.
 */
public final class ConfigUpdater {
    private final JavaPlugin plugin;
    private final File configFile;
    private final String currentVersion;
    private final List<ConfigUpdate> updates;

    public ConfigUpdater(JavaPlugin plugin, File configFile, String currentVersion) {
        this.plugin = plugin;
        this.configFile = configFile;
        this.currentVersion = currentVersion;
        this.updates = new ArrayList<>();
        registerUpdates();
    }

    /**
     * Registers all config updates by version.
     * Each update is applied if the config version is older than the update's target version.
     */
    private void registerUpdates() {
        // Update to version 1.1.0: Add allow-mace-enchanting option
        addUpdate("1.1.0", config -> {
            if (!config.contains("allow-mace-enchanting")) {
                config.set("allow-mace-enchanting", true);
            }
        });
    }

    /**
     * Adds a config update that will be applied when upgrading to the specified version.
     */
    private void addUpdate(String targetVersion, Consumer<YamlConfiguration> updateAction) {
        updates.add(new ConfigUpdate(targetVersion, updateAction));
    }

    /**
     * Updates the config file to the current version.
     * Reads the current config version, applies all necessary updates, and saves the file.
     */
    public void update() {
        if (!configFile.exists()) {
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        String configVersion = config.getString("version", "1.0");

        if (configVersion.equals(currentVersion)) {
            return; // Already up to date
        }

        plugin.getLogger().info("Updating config from version " + configVersion + " to " + currentVersion);

        // Apply all updates for versions newer than the config version
        for (ConfigUpdate update : updates) {
            if (isVersionNewer(update.targetVersion, configVersion)) {
                try {
                    update.updateAction.accept(config);
                    plugin.getLogger().info("Applied config update for version " + update.targetVersion);
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to apply config update for version " + update.targetVersion + ": " + e.getMessage());
                }
            }
        }

        // Update version in config
        config.set("version", currentVersion);

        // Save updated config
        try {
            config.save(configFile);
            plugin.getLogger().info("Config updated successfully to version " + currentVersion);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save updated config: " + e.getMessage());
        }
    }

    private boolean isVersionNewer(String version1, String version2) {
        String[] parts1 = version1.split("\\.");
        String[] parts2 = version2.split("\\.");

        int maxLength = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < maxLength; i++) {
            int v1 = i < parts1.length ? parseInt(parts1[i]) : 0;
            int v2 = i < parts2.length ? parseInt(parts2[i]) : 0;

            if (v1 > v2) return true;
            if (v1 < v2) return false;
        }
        return false; // Equal versions
    }

    private int parseInt(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Represents a single config update for a specific version.
     */
    private static final class ConfigUpdate {
        final String targetVersion;
        final Consumer<YamlConfiguration> updateAction;

        ConfigUpdate(String targetVersion, Consumer<YamlConfiguration> updateAction) {
            this.targetVersion = targetVersion;
            this.updateAction = updateAction;
        }
    }
}

