package org.bonkmc.limitedmaces.listeners;

import org.bonkmc.limitedmaces.LimitedMaces;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;
import java.util.UUID;

final class MacePickupRegistrar {
    private final LimitedMaces plugin;

    MacePickupRegistrar(LimitedMaces plugin) {
        this.plugin = plugin;
    }

    boolean registerPickup(EntityPickupItemEvent event, Player player, ItemStack stack) {
        Optional<UUID> trackedMaceId = plugin.registry().getTrackedId(stack);
        if (trackedMaceId.isPresent()) {
            return registerTaggedPickup(event, player, trackedMaceId.get());
        }

        return registerUntaggedPickup(event, player, stack);
    }

    private boolean registerTaggedPickup(EntityPickupItemEvent event, Player player, UUID maceId) {
        if (plugin.registry().getRecord(maceId) == null) {
            if (plugin.registry().getActiveCount() < plugin.cfg().getAllowedMaces()) {
                plugin.registry().ensureRegisteredExisting(maceId, player, player.getLocation(), "HELD");
            } else {
                removeIllegalPickup(event, player);
                return false;
            }
        }

        plugin.registry().updateLastSeen(maceId, player, player.getLocation(), "HELD");
        return true;
    }

    private boolean registerUntaggedPickup(EntityPickupItemEvent event, Player player, ItemStack stack) {
        if (plugin.registry().getActiveCount() < plugin.cfg().getAllowedMaces()) {
            UUID maceId = UUID.randomUUID();
            plugin.registry().tagWithId(stack, maceId);
            plugin.registry().ensureRegisteredExisting(maceId, player, player.getLocation(), "HELD");
            return true;
        }

        removeIllegalPickup(event, player);
        return false;
    }

    private void removeIllegalPickup(EntityPickupItemEvent event, Player player) {
        event.setCancelled(true);
        event.getItem().remove();
        player.sendMessage(plugin.cfg().msg("illegal-removed"));
        plugin.recipes().syncWithLimit();
    }
}
