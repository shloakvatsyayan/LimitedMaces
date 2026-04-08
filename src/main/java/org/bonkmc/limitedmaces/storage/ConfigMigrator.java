package org.bonkmc.limitedmaces.storage;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public final class ConfigMigrator {
    private final JavaPlugin plugin;
    private static final String MIGRATION_VERSION = "1.1.5";

    public ConfigMigrator(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void migrateIfNeeded() {
        File oldFolder = new File(plugin.getDataFolder().getParentFile(), "MultiMace");
        if (!oldFolder.exists()) {
            return;
        }

        plugin.getLogger().info("Found old MultiMace folder, starting migration...");

        try {
            migrateFromOldFolder(oldFolder);
            deleteOldFolder(oldFolder);
            plugin.getLogger().info("Migration completed successfully. Old MultiMace folder deleted.");
        } catch (Exception e) {
            plugin.getLogger().severe("Migration failed: " + e.getMessage());
        }
    }

    private void migrateFromOldFolder(File oldFolder) throws IOException {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        File oldConfigDir = new File(oldFolder, "config");
        File oldConfigFile = new File(oldConfigDir, "config.yml");
        if (oldConfigFile.exists()) {
            migrateConfig(oldConfigFile);
        }

        File oldDataDir = new File(oldFolder, "data");
        File oldDataFile = new File(oldDataDir, "maces.yml");
        if (oldDataFile.exists()) {
            migrateData(oldDataFile);
        }
    }

    private void migrateConfig(File oldConfigFile) throws IOException {
        File newConfigFile = new File(plugin.getDataFolder(), "config.yml");
        
        if (newConfigFile.exists()) {
            plugin.getLogger().info("Config already exists in new location, skipping config migration.");
            return;
        }

        YamlConfiguration oldConfig = YamlConfiguration.loadConfiguration(oldConfigFile);
        String version = oldConfig.getString("version", "1.0");

        if (isVersionOlderOrEqual(version, "1.1.3")) {
            plugin.getLogger().info("Migrating config from version " + version);

            String prefix = oldConfig.getString("messages.prefix", "&6[MultiMace]&r ");
            if (prefix.contains("MultiMace")) {
                oldConfig.set("messages.prefix", prefix.replace("MultiMace", "LimitedMaces"));
            }

            String reload = oldConfig.getString("messages.reload", "&aMultiMace config reloaded.");
            if (reload.contains("MultiMace")) {
                oldConfig.set("messages.reload", reload.replace("MultiMace", "LimitedMaces"));
            }

            oldConfig.set("version", MIGRATION_VERSION);
        }

        oldConfig.save(newConfigFile);
        plugin.getLogger().info("Config migrated to new location: " + newConfigFile.getPath());
    }

    private void migrateData(File oldDataFile) throws IOException {
        File newDataFile = new File(plugin.getDataFolder(), "maces.yml");
        
        if (newDataFile.exists()) {
            plugin.getLogger().info("Data file already exists in new location, skipping data migration.");
            return;
        }

        Files.copy(oldDataFile.toPath(), newDataFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        plugin.getLogger().info("Data migrated to new location: " + newDataFile.getPath());
    }

    private void deleteOldFolder(File folder) {
        if (folder == null || !folder.exists()) return;
        
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteOldFolder(file);
                } else {
                    file.delete();
                }
            }
        }
        folder.delete();
    }

    private boolean isVersionOlderOrEqual(String version, String target) {
        String[] parts1 = version.split("\\.");
        String[] parts2 = target.split("\\.");

        int maxLength = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < maxLength; i++) {
            int v1 = i < parts1.length ? parseInt(parts1[i]) : 0;
            int v2 = i < parts2.length ? parseInt(parts2[i]) : 0;

            if (v1 < v2) return true;
            if (v1 > v2) return false;
        }
        return true;
    }

    private int parseInt(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
