package org.bonkmc.limitedmaces.storage;

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
    private static final String CONFIG_VERSION = "1.1.4";

    private File file;
    private YamlConfiguration yaml;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        this.file = new File(plugin.getDataFolder(), "config.yml");

        if (!file.exists()) {
            boolean copied = copyEmbeddedToFile("config.yml", file);

            if (!copied) {
                YamlConfiguration def = new YamlConfiguration();
                def.set("version", CONFIG_VERSION);
                def.set("allowed-maces", 3);
                def.set("allow-mace-enchanting", true);
                def.set("messages.prefix", "&6[LimitedMaces]&r ");
                def.set("messages.crafted-broadcast", "&a%player% &7crafted &f%amount%&7 mace(s)! &7(%current%/%max%)");
                def.set("messages.destroyed-broadcast", "&cA mace was destroyed! &7(last held by &f%lastHolder%&7) &7(%current%/%max%)");
                def.set("messages.containers-blocked", "&cYou can't put the mace in any container.");
                def.set("messages.limit-reached", "&cMace limit reached. A mace must be destroyed before another can be crafted.");
                def.set("messages.illegal-removed", "&cAn illegal/untracked mace was removed.");
                def.set("messages.reload", "&aLimitedMaces config reloaded.");
                def.set("messages.no-permission", "&cYou don't have permission.");

                try {
                    def.save(file);
                } catch (IOException ignored) {}
            }
        }

        this.yaml = YamlConfiguration.loadConfiguration(file);

        ConfigUpdater updater = new ConfigUpdater(plugin, file, CONFIG_VERSION);
        updater.update();

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

    public void save() {
        if (yaml == null || file == null) return;
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save config.yml: " + e.getMessage());
        }
    }

    public int getAllowedMaces() {
        return Math.max(0, yaml.getInt("allowed-maces", 3));
    }

    public void setAllowedMaces(int count) {
        yaml.set("allowed-maces", Math.max(0, count));
        save();
    }

    public boolean isAllowMaceEnchanting() {
        return yaml.getBoolean("allow-mace-enchanting", true);
    }

    public void setAllowMaceEnchanting(boolean allow) {
        yaml.set("allow-mace-enchanting", allow);
        save();
    }

    public String msg(String key) {
        String prefix = color(yaml.getString("messages.prefix", "&6[LimitedMaces]&r "));
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
