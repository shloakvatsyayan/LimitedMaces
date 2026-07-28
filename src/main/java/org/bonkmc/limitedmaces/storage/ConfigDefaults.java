package org.bonkmc.limitedmaces.storage;

import org.bukkit.configuration.file.YamlConfiguration;

import java.util.Map;

final class ConfigDefaults {
    private static final Map<String, String> LIMITED_MACES_MESSAGES = Map.ofEntries(
            Map.entry("messages.limitedmaces-usage", "&cUsage: /limitedmaces <info|reload|update>"),
            Map.entry("messages.update-checking", "&7Checking Modrinth for the latest compatible version..."),
            Map.entry("messages.update-in-progress", "&eA LimitedMaces update is already in progress."),
            Map.entry(
                    "messages.update-no-compatible-version",
                    "&cNo LimitedMaces version supports Minecraft %game_version%."
            ),
            Map.entry("messages.update-up-to-date", "&aLimitedMaces is already up to date at version %version%."),
            Map.entry("messages.update-downloading", "&eDownloading LimitedMaces %version% from Modrinth..."),
            Map.entry(
                    "messages.update-success",
                    "&aLimitedMaces %version% was downloaded. Restart the server for the update to take effect."
            ),
            Map.entry("messages.update-failed", "&cLimitedMaces update failed: %error%")
    );

    private ConfigDefaults() {
    }

    static void addLimitedMacesMessages(YamlConfiguration configYaml) {
        LIMITED_MACES_MESSAGES.forEach((path, message) -> {
            if (!configYaml.contains(path)) {
                configYaml.set(path, message);
            }
        });
    }
}
