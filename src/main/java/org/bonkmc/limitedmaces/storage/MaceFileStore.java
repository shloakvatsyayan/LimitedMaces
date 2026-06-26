package org.bonkmc.limitedmaces.storage;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

final class MaceFileStore {
    private final JavaPlugin plugin;
    private final File maceFile;
    private YamlConfiguration maceYaml;

    MaceFileStore(JavaPlugin plugin) {
        this.plugin = plugin;
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        this.maceFile = new File(plugin.getDataFolder(), "maces.yml");
    }

    Map<UUID, MaceRecord> load() {
        if (!maceFile.exists()) {
            maceYaml = new YamlConfiguration();
            save(Collections.emptyList());
            return Collections.emptyMap();
        }

        maceYaml = YamlConfiguration.loadConfiguration(maceFile);
        Map<UUID, MaceRecord> loadedMaces = new HashMap<>();
        ConfigurationSection maceSection = maceYaml.getConfigurationSection("maces");
        if (maceSection == null) {
            return loadedMaces;
        }

        for (String maceKey : maceSection.getKeys(false)) {
            UUID maceId = readMaceId(maceKey);
            if (maceId == null) {
                continue;
            }

            ConfigurationSection recordSection = maceSection.getConfigurationSection(maceKey);
            if (recordSection == null) {
                plugin.getLogger().warning("Skipping malformed mace record: " + maceKey);
                continue;
            }

            loadedMaces.put(maceId, readRecord(maceId, recordSection));
        }

        return loadedMaces;
    }

    void save(Collection<MaceRecord> maceRecords) {
        if (maceYaml == null) {
            maceYaml = new YamlConfiguration();
        }

        maceYaml.set("maces", null);
        ConfigurationSection maceSection = maceYaml.createSection("maces");
        for (MaceRecord record : maceRecords) {
            writeRecord(maceSection, record);
        }

        try {
            maceYaml.save(maceFile);
        } catch (IOException exception) {
            plugin.getLogger().warning("Failed to save maces.yml: " + exception.getMessage());
        }
    }

    private MaceRecord readRecord(UUID maceId, ConfigurationSection recordSection) {
        MaceRecord record = new MaceRecord();
        record.id = maceId;
        record.createdBy = readOptionalUuid(recordSection, "createdBy");
        record.createdByName = recordSection.getString("createdByName", "Unknown");
        record.createdAt = recordSection.getLong("createdAt", 0L);
        record.lastHolder = readOptionalUuid(recordSection, "lastHolder");
        record.lastHolderName = recordSection.getString("lastHolderName", "Unknown");
        record.lastWorld = recordSection.getString("lastWorld", "");
        record.lastX = recordSection.getDouble("lastX", 0);
        record.lastY = recordSection.getDouble("lastY", 0);
        record.lastZ = recordSection.getDouble("lastZ", 0);
        record.lastSeenAt = recordSection.getLong("lastSeenAt", 0L);
        record.status = recordSection.getString("status", "UNKNOWN");
        record.isUntracked = recordSection.getBoolean("isUntracked", false);
        return record;
    }

    private void writeRecord(ConfigurationSection maceSection, MaceRecord record) {
        ConfigurationSection recordSection = maceSection.createSection(record.id.toString());
        recordSection.set("createdBy", record.createdBy == null ? null : record.createdBy.toString());
        recordSection.set("createdByName", record.createdByName);
        recordSection.set("createdAt", record.createdAt);
        recordSection.set("lastHolder", record.lastHolder == null ? null : record.lastHolder.toString());
        recordSection.set("lastHolderName", record.lastHolderName);
        recordSection.set("lastWorld", record.lastWorld);
        recordSection.set("lastX", record.lastX);
        recordSection.set("lastY", record.lastY);
        recordSection.set("lastZ", record.lastZ);
        recordSection.set("lastSeenAt", record.lastSeenAt);
        recordSection.set("status", record.status);
        recordSection.set("isUntracked", record.isUntracked);
    }

    private UUID readMaceId(String rawMaceId) {
        try {
            return UUID.fromString(rawMaceId);
        } catch (IllegalArgumentException exception) {
            plugin.getLogger().warning("Skipping mace record with invalid ID: " + rawMaceId);
            return null;
        }
    }

    private UUID readOptionalUuid(ConfigurationSection recordSection, String path) {
        String rawUuid = recordSection.getString(path);
        if (rawUuid == null || rawUuid.isBlank()) {
            return null;
        }

        try {
            return UUID.fromString(rawUuid);
        } catch (IllegalArgumentException exception) {
            plugin.getLogger().warning("Invalid UUID at " + recordSection.getCurrentPath() + "." + path + ": " + rawUuid);
            return null;
        }
    }
}
