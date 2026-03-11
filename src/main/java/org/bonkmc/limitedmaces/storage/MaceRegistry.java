package org.bonkmc.limitedmaces.storage;

import org.bonkmc.limitedmaces.LimitedMaces;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.ConfigurationSection;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.*;

public final class MaceRegistry {
    private final LimitedMaces plugin;

    private final NamespacedKey maceIdKey;
    private final Map<UUID, MaceRecord> active = new HashMap<>();

    private final File dataFile;
    private YamlConfiguration dataYaml;

    public MaceRegistry(LimitedMaces plugin) {
        this.plugin = plugin;
        this.maceIdKey = new NamespacedKey(plugin, "mace-id");

        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        this.dataFile = new File(plugin.getDataFolder(), "maces.yml");
    }

    public NamespacedKey getMaceIdKey() {
        return maceIdKey;
    }

    public MaceRecord getRecord(UUID id) {
        return active.get(id);
    }

    public int getActiveCount() {
        return (int) active.values().stream()
                .filter(r -> !r.isUntracked)
                .count();
    }

    public int getTotalCount() {
        return active.size();
    }

    public Collection<MaceRecord> getAll() {
        return Collections.unmodifiableCollection(active.values());
    }

    public boolean isMace(ItemStack it) {
        return it != null && it.getType() == Material.MACE;
    }

    public Optional<UUID> getTrackedId(ItemStack it) {
        if (!isMace(it)) return Optional.empty();
        ItemMeta meta = it.getItemMeta();
        if (meta == null) return Optional.empty();
        String raw = meta.getPersistentDataContainer().get(maceIdKey, PersistentDataType.STRING);
        if (raw == null || raw.isBlank()) return Optional.empty();
        try {
            return Optional.of(UUID.fromString(raw));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    public boolean isTrackedMace(ItemStack it) {
        return getTrackedId(it).isPresent();
    }

    public void load() {
        if (!dataFile.exists()) {
            this.dataYaml = new YamlConfiguration();
            save();
            return;
        }

        this.dataYaml = YamlConfiguration.loadConfiguration(dataFile);
        active.clear();

        ConfigurationSection maces = dataYaml.getConfigurationSection("maces");
        if (maces == null) return;

        for (String key : maces.getKeys(false)) {
            try {
                UUID id = UUID.fromString(key);
                ConfigurationSection sec = maces.getConfigurationSection(key);
                if (sec == null) continue;

                MaceRecord r = new MaceRecord();
                r.id = id;

                r.createdBy = readUuid(sec, "createdBy");
                r.createdByName = sec.getString("createdByName", "Unknown");
                r.createdAt = sec.getLong("createdAt", 0L);

                r.lastHolder = readUuid(sec, "lastHolder");
                r.lastHolderName = sec.getString("lastHolderName", "Unknown");

                r.lastWorld = sec.getString("lastWorld", "");
                r.lastX = sec.getDouble("lastX", 0);
                r.lastY = sec.getDouble("lastY", 0);
                r.lastZ = sec.getDouble("lastZ", 0);

                r.lastSeenAt = sec.getLong("lastSeenAt", 0L);
                r.status = sec.getString("status", "UNKNOWN");
                r.isUntracked = sec.getBoolean("isUntracked", false);

                active.put(id, r);
            } catch (Exception ignored) {
            }
        }
    }

    private UUID readUuid(ConfigurationSection sec, String path) {
        String s = sec.getString(path, null);
        if (s == null) return null;
        try {
            return UUID.fromString(s);
        } catch (Exception ex) {
            return null;
        }
    }

    public void save() {
        if (dataYaml == null) dataYaml = new YamlConfiguration();
        dataYaml.set("maces", null);

        ConfigurationSection maces = dataYaml.createSection("maces");
        for (MaceRecord r : active.values()) {
            ConfigurationSection sec = maces.createSection(r.id.toString());
            sec.set("createdBy", r.createdBy == null ? null : r.createdBy.toString());
            sec.set("createdByName", r.createdByName);
            sec.set("createdAt", r.createdAt);

            sec.set("lastHolder", r.lastHolder == null ? null : r.lastHolder.toString());
            sec.set("lastHolderName", r.lastHolderName);

            sec.set("lastWorld", r.lastWorld);
            sec.set("lastX", r.lastX);
            sec.set("lastY", r.lastY);
            sec.set("lastZ", r.lastZ);

            sec.set("lastSeenAt", r.lastSeenAt);
            sec.set("status", r.status);
            sec.set("isUntracked", r.isUntracked);
        }

        try {
            dataYaml.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save maces.yml: " + e.getMessage());
        }
    }

    public int remainingSlots() {
        return Math.max(0, plugin.cfg().getAllowedMaces() - getActiveCount());
    }

    public ItemStack createAndRegisterNewMace(Player owner, Location where) {
        return createAndRegisterNewMace(owner, where, false);
    }

    public ItemStack createAndRegisterNewMace(Player owner, Location where, boolean untracked) {
        UUID id = UUID.randomUUID();
        ItemStack mace = new ItemStack(Material.MACE, 1);
        tagWithId(mace, id);

        MaceRecord r = new MaceRecord();
        r.id = id;

        r.createdBy = owner.getUniqueId();
        r.createdByName = owner.getName();
        r.createdAt = Instant.now().toEpochMilli();

        r.lastHolder = owner.getUniqueId();
        r.lastHolderName = owner.getName();
        r.lastSeenAt = Instant.now().toEpochMilli();
        r.status = "HELD";
        r.isUntracked = untracked;
        r.setLocation(where);

        active.put(id, r);
        save();
        return mace;
    }

    public void ensureRegisteredExisting(UUID id, Player holder, Location where, String status) {
        MaceRecord r = active.get(id);
        if (r == null) {
            if (getActiveCount() >= plugin.cfg().getAllowedMaces()) {
                return;
            }
            r = new MaceRecord();
            r.id = id;
            r.createdBy = holder.getUniqueId();
            r.createdByName = holder.getName();
            r.createdAt = Instant.now().toEpochMilli();
            active.put(id, r);
        }

        r.lastHolder = holder.getUniqueId();
        r.lastHolderName = holder.getName();
        r.lastSeenAt = Instant.now().toEpochMilli();
        r.status = status;
        r.setLocation(where);
        save();
    }

    public void updateLastSeen(UUID id, Player holder, Location where, String status) {
        MaceRecord r = active.get(id);
        if (r == null) return;
        r.lastHolder = holder.getUniqueId();
        r.lastHolderName = holder.getName();
        r.lastSeenAt = Instant.now().toEpochMilli();
        r.status = status;
        r.setLocation(where);
        save();
    }

    public void updateDropped(UUID id, Location where, Player lastHolder) {
        MaceRecord r = active.get(id);
        if (r == null) return;
        if (lastHolder != null) {
            r.lastHolder = lastHolder.getUniqueId();
            r.lastHolderName = lastHolder.getName();
        }
        r.lastSeenAt = Instant.now().toEpochMilli();
        r.status = "DROPPED";
        r.setLocation(where);
        save();
    }

    public void removeTracked(UUID id, String reason) {
        if (active.remove(id) != null) {
            save();
            plugin.getLogger().info("Mace removed from tracking (" + reason + "): " + id);
        }
    }

    public void tagWithId(ItemStack mace, UUID id) {
        if (mace == null || mace.getType() != Material.MACE) return;
        ItemMeta meta = mace.getItemMeta();
        if (meta == null) return;
        
        String existing = meta.getPersistentDataContainer().get(maceIdKey, PersistentDataType.STRING);
        if (existing != null && existing.equals(id.toString())) {
            return;
        }
        
        meta.getPersistentDataContainer().set(maceIdKey, PersistentDataType.STRING, id.toString());
        mace.setItemMeta(meta);
    }

    public void scanAndNormalizePlayerInventory(Player p) {
        if (p == null) return;

        PlayerInventory inv = p.getInventory();
        List<ItemStack> all = new ArrayList<>();

        all.addAll(Arrays.asList(inv.getContents()));
        all.add(inv.getItemInOffHand());

        Set<UUID> seenIds = new HashSet<>();

        for (ItemStack it : all) {
            if (!isMace(it)) continue;

            Optional<UUID> maybe = getTrackedId(it);
            if (maybe.isPresent()) {
                UUID id = maybe.get();

                if (seenIds.contains(id)) {
                    removeOneFromInventory(p, id);
                    p.sendMessage(plugin.cfg().msg("illegal-removed"));
                    continue;
                }
                seenIds.add(id);

                if (!active.containsKey(id)) {
                    if (getActiveCount() < plugin.cfg().getAllowedMaces()) {
                        ensureRegisteredExisting(id, p, p.getLocation(), "HELD");
                    } else {
                        removeOneFromInventory(p, id);
                        p.sendMessage(plugin.cfg().msg("illegal-removed"));
                    }
                } else {
                    MaceRecord record = active.get(id);
                    if (record != null && !"HELD".equals(record.status)) {
                        updateLastSeen(id, p, p.getLocation(), "HELD");
                    }
                }
            } else {
                if (getActiveCount() < plugin.cfg().getAllowedMaces()) {
                    UUID id = UUID.randomUUID();
                    tagWithId(it, id);
                    ensureRegisteredExisting(id, p, p.getLocation(), "HELD");
                } else {
                    removeFirstUntrackedMace(p);
                    p.sendMessage(plugin.cfg().msg("illegal-removed"));
                }
            }
        }
    }

    private void removeFirstUntrackedMace(Player p) {
        PlayerInventory inv = p.getInventory();
        ItemStack[] contents = inv.getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack it = contents[i];
            if (isMace(it) && getTrackedId(it).isEmpty()) {
                contents[i] = null;
                inv.setContents(contents);
                return;
            }
        }
        ItemStack off = inv.getItemInOffHand();
        if (isMace(off) && getTrackedId(off).isEmpty()) {
            inv.setItemInOffHand(null);
        }
    }

    private void removeOneFromInventory(Player p, UUID id) {
        PlayerInventory inv = p.getInventory();

        ItemStack[] contents = inv.getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack it = contents[i];
            if (it == null || it.getType() != Material.MACE) continue;
            Optional<UUID> maybe = getTrackedId(it);
            if (maybe.isPresent() && maybe.get().equals(id)) {
                contents[i] = null;
                inv.setContents(contents);
                return;
            }
        }

        ItemStack off = inv.getItemInOffHand();
        if (off != null && off.getType() == Material.MACE) {
            Optional<UUID> maybe = getTrackedId(off);
            if (maybe.isPresent() && maybe.get().equals(id)) {
                inv.setItemInOffHand(null);
            }
        }
    }

    public String prettyHolder(UUID holder) {
        if (holder == null) return "Unknown";
        OfflinePlayer op = Bukkit.getOfflinePlayer(holder);
        String name = op.getName();
        return name != null ? name : holder.toString();
    }

    public Collection<MaceRecord> getUntrackedMaces() {
        return active.values().stream()
                .filter(r -> r.isUntracked)
                .collect(java.util.stream.Collectors.toList());
    }

    public void clearUntrackedMaces() {
        List<UUID> toRemove = active.values().stream()
                .filter(r -> r.isUntracked)
                .map(r -> r.id)
                .collect(java.util.stream.Collectors.toList());
        
        for (UUID id : toRemove) {
            active.remove(id);
        }
        
        if (!toRemove.isEmpty()) {
            save();
        }
    }
}
