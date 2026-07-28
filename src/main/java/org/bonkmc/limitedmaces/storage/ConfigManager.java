package org.bonkmc.limitedmaces.storage;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Map;

public final class ConfigManager {
    private static final String CONFIG_VERSION = "1.2.1";

    private final JavaPlugin plugin;
    private File configFile;
    private YamlConfiguration configYaml;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        this.configFile = new File(plugin.getDataFolder(), "config.yml");

        if (!configFile.exists()) {
            boolean copied = copyEmbeddedToFile("config.yml", configFile);

            if (!copied) {
                YamlConfiguration fallbackConfig = new YamlConfiguration();
                fallbackConfig.set("version", CONFIG_VERSION);
                fallbackConfig.set("allowed-maces", 3);
                fallbackConfig.set("allow-mace-enchanting", true);
                fallbackConfig.set("block-container-storage", true);
                fallbackConfig.set("messages.prefix", "&6[LimitedMaces]&r ");
                fallbackConfig.set("messages.crafted-broadcast", "&a%player% &7crafted &f%amount%&7 mace(s)! &7(%current%/%max%)");
                fallbackConfig.set("messages.destroyed-broadcast", "&cA mace was destroyed! &7(last held by &f%lastHolder%&7) &7(%current%/%max%)");
                fallbackConfig.set("messages.containers-blocked", "&cYou can't put the mace in any container.");
                fallbackConfig.set("messages.limit-reached", "&cMace limit reached. A mace must be destroyed before another can be crafted.");
                fallbackConfig.set("messages.illegal-removed", "&cAn illegal/untracked mace was removed.");
                fallbackConfig.set("messages.reload", "&aLimitedMaces config reloaded.");
                fallbackConfig.set("messages.no-permission", "&cYou don't have permission.");
                ConfigDefaults.addLimitedMacesMessages(fallbackConfig);

                try {
                    fallbackConfig.save(configFile);
                } catch (IOException exception) {
                    plugin.getLogger().severe("Failed to write fallback config.yml: " + exception.getMessage());
                }
            }
        }

        this.configYaml = YamlConfiguration.loadConfiguration(configFile);

        ConfigUpdater updater = new ConfigUpdater(plugin, configFile, CONFIG_VERSION);
        updater.update();

        this.configYaml = YamlConfiguration.loadConfiguration(configFile);
    }

    private boolean copyEmbeddedToFile(String resourcePath, File targetFile) {
        try (InputStream in = plugin.getResource(resourcePath)) {
            if (in == null) return false;
            Files.copy(in, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException exception) {
            plugin.getLogger().warning("Failed to copy embedded resource '" + resourcePath + "': " + exception.getMessage());
            return false;
        }
    }

    public void reload() {
        load();
    }

    public void save() {
        if (configYaml == null || configFile == null) return;
        try {
            configYaml.save(configFile);
        } catch (IOException exception) {
            plugin.getLogger().warning("Failed to save config.yml: " + exception.getMessage());
        }
    }

    public int getAllowedMaces() {
        return Math.max(0, configYaml.getInt("allowed-maces", 3));
    }

    public void setAllowedMaces(int count) {
        configYaml.set("allowed-maces", Math.max(0, count));
        save();
    }

    public boolean isAllowMaceEnchanting() {
        return configYaml.getBoolean("allow-mace-enchanting", true);
    }

    public void setAllowMaceEnchanting(boolean allow) {
        configYaml.set("allow-mace-enchanting", allow);
        save();
    }

    public boolean isBlockContainerStorage() {
        return configYaml.getBoolean("block-container-storage", true);
    }

    public String msg(String key) {
        return msg(key, Map.of());
    }

    public String msg(String key, Map<String, String> placeholders) {
        String prefix = color(configYaml.getString("messages.prefix", "&6[LimitedMaces]&r "));
        String rawMessage = configYaml.getString("messages." + key, "");
        if (rawMessage == null) rawMessage = "";
        for (Map.Entry<String, String> placeholder : placeholders.entrySet()) {
            rawMessage = rawMessage.replace(placeholder.getKey(), placeholder.getValue());
        }
        rawMessage = rawMessage.replace("\\n", "\n");
        return prefix + color(rawMessage);
    }

    public String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message == null ? "" : message);
    }
}
