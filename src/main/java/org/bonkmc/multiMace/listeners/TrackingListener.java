package org.bonkmc.multiMace.listeners;

import org.bonkmc.multiMace.MultiMace;
import org.bonkmc.multiMace.storage.MaceRecord;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
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

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class TrackingListener implements Listener {
    private final MultiMace plugin;
    private final Set<UUID> pickedUpItems = new HashSet<>();
    private final Set<UUID> naturallyDespawnedItems = new HashSet<>();

    public TrackingListener(MultiMace plugin) {
        this.plugin = plugin;
    }

    private boolean isMace(ItemStack it) {
        return it != null && it.getType() == Material.MACE;
    }

    private void broadcastDestroyed(UUID id, String reason) {
        MaceRecord r = plugin.registry().getRecord(id);
        if (r == null) return;
        
        // Don't broadcast destruction messages for untracked maces
        if (r.isUntracked) {
            plugin.registry().removeTracked(id, reason);
            plugin.recipes().syncWithLimit();
            return;
        }
        
        String lastHolder = (r.lastHolderName != null && !r.lastHolderName.isBlank())
                ? r.lastHolderName
                : "Unknown";

        plugin.registry().removeTracked(id, reason);
        plugin.recipes().syncWithLimit();

        Bukkit.broadcastMessage(plugin.cfg().msg("destroyed-broadcast")
                .replace("%lastHolder%", lastHolder)
                .replace("%reason%", reason)
                .replace("%current%", String.valueOf(plugin.registry().getActiveCount()))
                .replace("%max%", String.valueOf(plugin.cfg().getAllowedMaces())));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent e) {
        plugin.registry().scanAndNormalizePlayerInventory(e.getPlayer());
        plugin.recipes().syncWithLimit();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onQuit(PlayerQuitEvent e) {
        plugin.registry().scanAndNormalizePlayerInventory(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent e) {
        Item dropped = e.getItemDrop();
        ItemStack stack = dropped.getItemStack();
        if (!isMace(stack)) return;

        Optional<UUID> id = plugin.registry().getTrackedId(stack);
        if (id.isPresent()) {
            plugin.registry().updateDropped(id.get(), dropped.getLocation(), e.getPlayer());
        } else {
            if (plugin.registry().getActiveCount() < plugin.cfg().getAllowedMaces()) {
                UUID newId = UUID.randomUUID();
                plugin.registry().tagWithId(stack, newId);
                plugin.registry().ensureRegisteredExisting(newId, e.getPlayer(), dropped.getLocation(), "DROPPED");
            } else {
                dropped.remove();
                e.getPlayer().sendMessage(plugin.cfg().msg("illegal-removed"));
            }
        }

        plugin.recipes().syncWithLimit();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent e) {
        Entity ent = e.getEntity();
        if (!(ent instanceof Player p)) return;

        ItemStack stack = e.getItem().getItemStack();
        if (!isMace(stack)) return;

        // Mark this item as being picked up to prevent EntityRemoveEvent from counting it as destroyed
        UUID itemEntityId = e.getItem().getUniqueId();
        pickedUpItems.add(itemEntityId);
        
        // Remove from set after a short delay to prevent memory leaks
        Bukkit.getScheduler().runTaskLater(plugin, () -> pickedUpItems.remove(itemEntityId), 5L);

        Optional<UUID> id = plugin.registry().getTrackedId(stack);
        if (id.isPresent()) {
            UUID mid = id.get();
            if (plugin.registry().getRecord(mid) == null) {
                if (plugin.registry().getActiveCount() < plugin.cfg().getAllowedMaces()) {
                    plugin.registry().ensureRegisteredExisting(mid, p, p.getLocation(), "HELD");
                } else {
                    e.setCancelled(true);
                    e.getItem().remove();
                    p.sendMessage(plugin.cfg().msg("illegal-removed"));
                    plugin.recipes().syncWithLimit();
                    return;
                }
            }
            plugin.registry().updateLastSeen(mid, p, p.getLocation(), "HELD");
        } else {
            if (plugin.registry().getActiveCount() < plugin.cfg().getAllowedMaces()) {
                UUID newId = UUID.randomUUID();
                plugin.registry().tagWithId(stack, newId);
                plugin.registry().ensureRegisteredExisting(newId, p, p.getLocation(), "HELD");
            } else {
                e.setCancelled(true);
                e.getItem().remove();
                p.sendMessage(plugin.cfg().msg("illegal-removed"));
                plugin.recipes().syncWithLimit();
                return;
            }
        }

        plugin.recipes().syncWithLimit();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDespawn(ItemDespawnEvent e) {
        ItemStack stack = e.getEntity().getItemStack();
        if (!isMace(stack)) return;

        UUID itemEntityId = e.getEntity().getUniqueId();
        naturallyDespawnedItems.add(itemEntityId);
        
        // Remove from set after a delay to prevent memory leaks
        Bukkit.getScheduler().runTaskLater(plugin, () -> naturallyDespawnedItems.remove(itemEntityId), 5L);

        plugin.registry().getTrackedId(stack).ifPresent(id -> broadcastDestroyed(id, "despawn"));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCombust(EntityCombustEvent e) {
        if (!(e.getEntity() instanceof Item item)) return;
        ItemStack stack = item.getItemStack();
        if (!isMace(stack)) return;

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!item.isValid() || item.isDead()) {
                plugin.registry().getTrackedId(stack).ifPresent(id -> broadcastDestroyed(id, "combust"));
            }
        }, 1L);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Item item)) return;
        ItemStack stack = item.getItemStack();
        if (!isMace(stack)) return;

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!item.isValid() || item.isDead()) {
                plugin.registry().getTrackedId(stack).ifPresent(id -> broadcastDestroyed(id, "damage:" + e.getCause().name()));
            }
        }, 1L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRemove(EntityRemoveEvent e) {
        if (!(e.getEntity() instanceof Item item)) return;
        
        UUID itemEntityId = item.getUniqueId();
        
        // Ignore if this item was just picked up (don't count pickup as destruction)
        if (pickedUpItems.contains(itemEntityId)) {
            return;
        }
        
        // Ignore if this item naturally despawned (already handled by onDespawn)
        if (naturallyDespawnedItems.contains(itemEntityId)) {
            return;
        }
        
        ItemStack stack = item.getItemStack();
        if (stack == null || !isMace(stack)) return;

        // This catches /kill command and other forced removals that aren't natural despawns
        Optional<UUID> idOpt = plugin.registry().getTrackedId(stack);
        if (idOpt.isPresent()) {
            UUID id = idOpt.get();
            // Only broadcast if mace is still tracked
            MaceRecord record = plugin.registry().getRecord(id);
            if (record != null) {
                broadcastDestroyed(id, "removed");
            }
        }
    }
}
