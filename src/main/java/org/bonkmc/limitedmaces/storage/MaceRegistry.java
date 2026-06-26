package org.bonkmc.limitedmaces.storage;

import org.bonkmc.limitedmaces.LimitedMaces;
import org.bonkmc.limitedmaces.items.MaceItems;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public final class MaceRegistry {
    private final LimitedMaces plugin;
    private final Map<UUID, MaceRecord> activeMaces = new HashMap<>();
    private final MaceIdTagger maceIdTagger;
    private final MaceFileStore maceFileStore;
    private final MaceInventoryNormalizer inventoryNormalizer;

    public MaceRegistry(LimitedMaces plugin) {
        this.plugin = plugin;
        this.maceIdTagger = new MaceIdTagger(plugin);
        this.maceFileStore = new MaceFileStore(plugin);
        this.inventoryNormalizer = new MaceInventoryNormalizer(plugin, this);
    }

    public MaceRecord getRecord(UUID maceId) {
        return activeMaces.get(maceId);
    }

    public int getActiveCount() {
        return (int) activeMaces.values().stream()
                .filter(record -> !record.isUntracked)
                .count();
    }

    public Collection<MaceRecord> getAll() {
        return Collections.unmodifiableCollection(activeMaces.values());
    }

    public boolean isMace(ItemStack stack) {
        return MaceItems.isMace(stack);
    }

    public Optional<UUID> getTrackedId(ItemStack stack) {
        return maceIdTagger.readId(stack);
    }

    public boolean isTrackedMace(ItemStack stack) {
        return getTrackedId(stack).isPresent();
    }

    public void load() {
        activeMaces.clear();
        activeMaces.putAll(maceFileStore.load());
    }

    public void save() {
        maceFileStore.save(activeMaces.values());
    }

    public ItemStack createAndRegisterNewMace(Player owner, Location location) {
        return createAndRegisterNewMace(owner, location, false);
    }

    public ItemStack createAndRegisterNewMace(Player owner, Location location, boolean isUntracked) {
        UUID maceId = UUID.randomUUID();
        ItemStack mace = new ItemStack(Material.MACE, 1);
        tagWithId(mace, maceId);

        long now = Instant.now().toEpochMilli();
        MaceRecord record = new MaceRecord();
        record.id = maceId;
        record.createdBy = owner.getUniqueId();
        record.createdByName = owner.getName();
        record.createdAt = now;
        record.lastHolder = owner.getUniqueId();
        record.lastHolderName = owner.getName();
        record.lastSeenAt = now;
        record.status = "HELD";
        record.isUntracked = isUntracked;
        record.setLocation(location);

        activeMaces.put(maceId, record);
        save();
        return mace;
    }

    public void ensureRegisteredExisting(UUID maceId, Player holder, Location location, String status) {
        MaceRecord record = activeMaces.get(maceId);
        if (record == null) {
            if (getActiveCount() >= plugin.cfg().getAllowedMaces()) {
                return;
            }
            record = new MaceRecord();
            record.id = maceId;
            record.createdBy = holder.getUniqueId();
            record.createdByName = holder.getName();
            record.createdAt = Instant.now().toEpochMilli();
            activeMaces.put(maceId, record);
        }

        record.lastHolder = holder.getUniqueId();
        record.lastHolderName = holder.getName();
        record.lastSeenAt = Instant.now().toEpochMilli();
        record.status = status;
        record.setLocation(location);
        save();
    }

    public void updateLastSeen(UUID maceId, Player holder, Location location, String status) {
        MaceRecord record = activeMaces.get(maceId);
        if (record == null) {
            return;
        }

        record.lastHolder = holder.getUniqueId();
        record.lastHolderName = holder.getName();
        record.lastSeenAt = Instant.now().toEpochMilli();
        record.status = status;
        record.setLocation(location);
        save();
    }

    public void updateDropped(UUID maceId, Location location, Player lastHolder) {
        MaceRecord record = activeMaces.get(maceId);
        if (record == null) {
            return;
        }

        if (lastHolder != null) {
            record.lastHolder = lastHolder.getUniqueId();
            record.lastHolderName = lastHolder.getName();
        }
        record.lastSeenAt = Instant.now().toEpochMilli();
        record.status = "DROPPED";
        record.setLocation(location);
        save();
    }

    public void removeTracked(UUID maceId, String reason) {
        if (activeMaces.remove(maceId) != null) {
            save();
            plugin.getLogger().info("Mace removed from tracking (" + reason + "): " + maceId);
        }
    }

    public void tagWithId(ItemStack mace, UUID maceId) {
        maceIdTagger.tag(mace, maceId);
    }

    public void scanAndNormalizePlayerInventory(Player player) {
        inventoryNormalizer.scanAndNormalize(player);
    }

    public Collection<MaceRecord> getUntrackedMaces() {
        return activeMaces.values().stream()
                .filter(record -> record.isUntracked)
                .collect(Collectors.toList());
    }

    public void clearUntrackedMaces() {
        List<UUID> untrackedMaceIds = activeMaces.values().stream()
                .filter(record -> record.isUntracked)
                .map(record -> record.id)
                .collect(Collectors.toList());
        
        for (UUID maceId : untrackedMaceIds) {
            activeMaces.remove(maceId);
        }
        
        if (!untrackedMaceIds.isEmpty()) {
            save();
        }
    }
}
