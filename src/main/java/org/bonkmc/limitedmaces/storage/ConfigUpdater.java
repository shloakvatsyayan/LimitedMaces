package org.bonkmc.limitedmaces.storage;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

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

    private void registerUpdates() {
        addUpdate("1.1.0", config -> {
            if (!config.contains("allow-mace-enchanting")) {
                config.set("allow-mace-enchanting", true);
            }
        });

        addUpdate("1.1.4", config -> {
            String prefix = config.getString("messages.prefix", "");
            if (prefix.contains("MultiMace")) {
                config.set("messages.prefix", prefix.replace("MultiMace", "LimitedMaces"));
            }
            
            String reload = config.getString("messages.reload", "");
            if (reload.contains("MultiMace")) {
                config.set("messages.reload", reload.replace("MultiMace", "LimitedMaces"));
            }
        });

        addUpdate("1.1.5", config -> {
            if (!config.contains("block-container-storage")) {
                config.set("block-container-storage", true);
            }
        });
    }

    private void addUpdate(String targetVersion, Consumer<YamlConfiguration> updateAction) {
        updates.add(new ConfigUpdate(targetVersion, updateAction));
    }

    public void update() {
        if (!configFile.exists()) {
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        String configVersion = config.getString("version", "1.0");

        if (configVersion.equals(currentVersion)) {
            return;
        }

        plugin.getLogger().info("Updating config from version " + configVersion + " to " + currentVersion);

        for (ConfigUpdate update : updates) {
            if (VersionComparator.isNewer(update.targetVersion(), configVersion)) {
                try {
                    update.apply(config);
                    plugin.getLogger().info("Applied config update for version " + update.targetVersion());
                } catch (RuntimeException exception) {
                    plugin.getLogger().warning("Failed to apply config update for version " + update.targetVersion() + ": " + exception.getMessage());
                }
            }
        }

        config.set("version", currentVersion);

        try {
            config.save(configFile);
            plugin.getLogger().info("Config updated successfully to version " + currentVersion);
        } catch (IOException exception) {
            plugin.getLogger().severe("Failed to save updated config: " + exception.getMessage());
        }
    }
}
