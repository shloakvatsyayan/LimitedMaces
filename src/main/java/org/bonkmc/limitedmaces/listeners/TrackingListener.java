package org.bonkmc.limitedmaces.listeners;

import org.bonkmc.limitedmaces.LimitedMaces;
import org.bonkmc.limitedmaces.items.MaceItems;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;
import java.util.UUID;

public final class TrackingListener implements Listener {
    private final LimitedMaces plugin;
    private final MaceDestructionNotifier destructionNotifier;
    private final TransientItemRemovalTracker removalTracker;
    private final MacePickupRegistrar pickupRegistrar;

    public TrackingListener(LimitedMaces plugin) {
        this.plugin = plugin;
        this.destructionNotifier = new MaceDestructionNotifier(plugin);
        this.removalTracker = new TransientItemRemovalTracker(plugin);
        this.pickupRegistrar = new MacePickupRegistrar(plugin);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent event) {
        plugin.registry().scanAndNormalizePlayerInventory(event.getPlayer());
        plugin.recipes().syncWithLimit();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onQuit(PlayerQuitEvent event) {
        plugin.registry().scanAndNormalizePlayerInventory(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        Item droppedMace = event.getItemDrop();
        ItemStack stack = droppedMace.getItemStack();
        if (!MaceItems.isMace(stack)) {
            return;
        }

        Optional<UUID> trackedMaceId = plugin.registry().getTrackedId(stack);
        if (trackedMaceId.isPresent()) {
            plugin.registry().updateDropped(trackedMaceId.get(), droppedMace.getLocation(), event.getPlayer());
        } else {
            if (plugin.registry().getActiveCount() < plugin.cfg().getAllowedMaces()) {
                UUID maceId = UUID.randomUUID();
                plugin.registry().tagWithId(stack, maceId);
                plugin.registry().ensureRegisteredExisting(maceId, event.getPlayer(), droppedMace.getLocation(), "DROPPED");
            } else {
                droppedMace.remove();
                event.getPlayer().sendMessage(plugin.cfg().msg("illegal-removed"));
            }
        }

        plugin.recipes().syncWithLimit();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        Item droppedMace = event.getItem();
        ItemStack stack = droppedMace.getItemStack();
        if (!MaceItems.isMace(stack)) {
            return;
        }

        removalTracker.rememberPickup(droppedMace);

        if (!pickupRegistrar.registerPickup(event, player, stack)) {
            return;
        }

        plugin.recipes().syncWithLimit();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDespawn(ItemDespawnEvent event) {
        Item droppedMace = event.getEntity();
        ItemStack stack = droppedMace.getItemStack();
        if (!MaceItems.isMace(stack)) {
            return;
        }

        removalTracker.rememberDespawn(droppedMace);
        plugin.registry().getTrackedId(stack).ifPresent(maceId -> destructionNotifier.broadcastDestroyed(maceId, "despawn"));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCombust(EntityCombustEvent event) {
        if (!(event.getEntity() instanceof Item droppedMace)) {
            return;
        }

        ItemStack stack = droppedMace.getItemStack();
        if (!MaceItems.isMace(stack)) {
            return;
        }

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!droppedMace.isValid() || droppedMace.isDead()) {
                plugin.registry().getTrackedId(stack).ifPresent(maceId -> destructionNotifier.broadcastDestroyed(maceId, "combust"));
            }
        }, 1L);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Item droppedMace)) {
            return;
        }

        ItemStack stack = droppedMace.getItemStack();
        if (!MaceItems.isMace(stack)) {
            return;
        }

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!droppedMace.isValid() || droppedMace.isDead()) {
                plugin.registry().getTrackedId(stack).ifPresent(maceId -> destructionNotifier.broadcastDestroyed(maceId, "damage:" + event.getCause().name()));
            }
        }, 1L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRemove(EntityRemoveEvent event) {
        if (!(event.getEntity() instanceof Item droppedMace)) {
            return;
        }

        if (removalTracker.shouldIgnoreRemoval(droppedMace)) {
            return;
        }
        
        ItemStack stack = droppedMace.getItemStack();
        if (!MaceItems.isMace(stack)) {
            return;
        }

        Optional<UUID> trackedMaceId = plugin.registry().getTrackedId(stack);
        if (trackedMaceId.isPresent()) {
            UUID maceId = trackedMaceId.get();
            if (plugin.registry().getRecord(maceId) != null) {
                destructionNotifier.broadcastDestroyed(maceId, "removed");
            }
        }
    }
}
