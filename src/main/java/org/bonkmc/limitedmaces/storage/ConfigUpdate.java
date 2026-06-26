package org.bonkmc.limitedmaces.storage;

import org.bukkit.configuration.file.YamlConfiguration;

import java.util.function.Consumer;

final class ConfigUpdate {
    private final String targetVersion;
    private final Consumer<YamlConfiguration> updateAction;

    ConfigUpdate(String targetVersion, Consumer<YamlConfiguration> updateAction) {
        this.targetVersion = targetVersion;
        this.updateAction = updateAction;
    }

    String targetVersion() {
        return targetVersion;
    }

    void apply(YamlConfiguration configYaml) {
        updateAction.accept(configYaml);
    }
}
