package org.bonkmc.limitedmaces.listeners;

import org.bonkmc.limitedmaces.LimitedMaces;
import org.bonkmc.limitedmaces.items.MaceItems;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.CrafterCraftEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;

public final class CraftingListener implements Listener {
    private final LimitedMaces plugin;
    private final MaceCraftHandler craftHandler;

    public CraftingListener(LimitedMaces plugin) {
        this.plugin = plugin;
        this.craftHandler = new MaceCraftHandler(plugin);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepare(PrepareItemCraftEvent event) {
        if (event.getRecipe() == null) return;
        ItemStack recipeResult = event.getRecipe().getResult();
        if (!MaceItems.isMace(recipeResult)) return;

        if (plugin.registry().getActiveCount() >= plugin.cfg().getAllowedMaces()) {
            event.getInventory().setResult(new ItemStack(Material.AIR));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (event.getRecipe() == null) return;
        ItemStack recipeResult = event.getRecipe().getResult();
        if (!MaceItems.isMace(recipeResult)) return;

        if (!(event.getWhoClicked() instanceof Player player)) return;

        int allowed = plugin.cfg().getAllowedMaces();
        int active = plugin.registry().getActiveCount();
        int remaining = Math.max(0, allowed - active);

        if (remaining <= 0) {
            event.setCancelled(true);
            player.sendMessage(plugin.cfg().msg("limit-reached"));
            plugin.recipes().syncWithLimit();
            return;
        }

        event.setCancelled(true);

        if (event.isShiftClick()) {
            craftHandler.craftIntoInventory(player, event.getInventory(), remaining, allowed);
            return;
        }

        craftHandler.craftSingle(player, event, allowed);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCrafter(CrafterCraftEvent event) {
        ItemStack recipeResult = event.getResult();
        if (!MaceItems.isMace(recipeResult)) return;

        event.setCancelled(true);
        event.setResult(new ItemStack(Material.AIR));
    }
}
