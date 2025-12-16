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
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;
import java.util.UUID;

public final class TrackingListener implements Listener {
    private final MultiMace plugin;

    public TrackingListener(MultiMace plugin) {
        this.plugin = plugin;
    }

    private boolean isMace(ItemStack it) {
        return it != null && it.getType() == Material.MACE;
    }

    private void broadcastDestroyed(UUID id, String reason) {
        MaceRecord r = plugin.registry().getRecord(id);
        String lastHolder = (r != null && r.lastHolderName != null && !r.lastHolderName.isBlank())
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
}
