package org.bonkmc.multiMace.storage;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public final class ConfigManager {
    private final JavaPlugin plugin;
    private static final String CONFIG_VERSION = "1.1.1";

    private File file;
    private YamlConfiguration yaml;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        // config/config.yml on disk (folder requested)
        File configDir = new File(plugin.getDataFolder(), "config");
        if (!configDir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            configDir.mkdirs();
        }

        this.file = new File(configDir, "config.yml");

        if (!file.exists()) {
            // Support either embedded resource path:
            // - src/main/resources/config.yml  (what you currently have)
            // - src/main/resources/config/config.yml (also supported if you add it later)
            boolean copied =
                    copyEmbeddedToFile("config/config.yml", file) ||
                            copyEmbeddedToFile("config.yml", file);

            if (!copied) {
                // Last-resort: create a minimal default config if no embedded file exists
                YamlConfiguration def = new YamlConfiguration();
                def.set("version", CONFIG_VERSION);
                def.set("allowed-maces", 3);
                def.set("allow-mace-enchanting", true);
                def.set("messages.prefix", "&6[MultiMace]&r ");
                def.set("messages.crafted", "&aMace crafted! &7(%current%/%max%)");
                def.set("messages.limit-reached", "&cMace limit reached. A mace must be destroyed before another can be crafted.");
                def.set("messages.containers-blocked", "&cYou can't put the mace in any container.");
                def.set("messages.illegal-removed", "&cAn illegal/untracked mace was removed.");
                def.set("messages.reload", "&aMultiMace config reloaded.");
                def.set("messages.no-permission", "&cYou don't have permission.");

                try {
                    def.save(file);
                } catch (IOException ignored) {}
            }
        }

        this.yaml = YamlConfiguration.loadConfiguration(file);

        // Update config to current version if needed
        ConfigUpdater updater = new ConfigUpdater(plugin, file, CONFIG_VERSION);
        updater.update();

        // Reload config after update
        this.yaml = YamlConfiguration.loadConfiguration(file);
    }

    private boolean copyEmbeddedToFile(String resourcePath, File targetFile) {
        try (InputStream in = plugin.getResource(resourcePath)) {
            if (in == null) return false;
            Files.copy(in, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to copy embedded resource '" + resourcePath + "': " + e.getMessage());
            return false;
        }
    }

    public void reload() {
        load();
    }

    public int getAllowedMaces() {
        return Math.max(0, yaml.getInt("allowed-maces", 3));
    }

    public boolean isAllowMaceEnchanting() {
        return yaml.getBoolean("allow-mace-enchanting", true);
    }

    public String msg(String key) {
        String prefix = color(yaml.getString("messages.prefix", "&6[MultiMace]&r "));
        String raw = yaml.getString("messages." + key, "");
        if (raw == null) raw = "";
        raw = raw.replace("\\n", "\n");
        return prefix + color(raw);
    }

    public String msgNoPrefix(String key) {
        String raw = yaml.getString("messages." + key, "");
        if (raw == null) raw = "";
        raw = raw.replace("\\n", "\n");
        return color(raw);
    }

    public String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s == null ? "" : s);
    }
}
