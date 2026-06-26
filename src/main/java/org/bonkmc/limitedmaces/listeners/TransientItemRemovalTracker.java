package org.bonkmc.limitedmaces.listeners;

import org.bonkmc.limitedmaces.LimitedMaces;
import org.bukkit.entity.Item;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

final class TransientItemRemovalTracker {
    private final LimitedMaces plugin;
    private final Set<UUID> pickedUpEntityIds = new HashSet<>();
    private final Set<UUID> despawnedEntityIds = new HashSet<>();

    TransientItemRemovalTracker(LimitedMaces plugin) {
        this.plugin = plugin;
    }

    void rememberPickup(Item droppedMace) {
        remember(pickedUpEntityIds, droppedMace.getUniqueId());
    }

    void rememberDespawn(Item droppedMace) {
        remember(despawnedEntityIds, droppedMace.getUniqueId());
    }

    boolean shouldIgnoreRemoval(Item droppedMace) {
        UUID entityId = droppedMace.getUniqueId();
        return pickedUpEntityIds.contains(entityId) || despawnedEntityIds.contains(entityId);
    }

    private void remember(Set<UUID> entityIds, UUID entityId) {
        entityIds.add(entityId);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> entityIds.remove(entityId), 5L);
    }
}
