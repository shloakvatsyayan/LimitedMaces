package org.bonkmc.limitedmaces.listeners;

import org.bonkmc.limitedmaces.LimitedMaces;
import org.bonkmc.limitedmaces.items.MaceItems;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.CrafterCraftEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;

public final class CraftingListener implements Listener {
    private final LimitedMaces plugin;

    public CraftingListener(LimitedMaces plugin) {
        this.plugin = plugin;
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

        if (event.isShiftClick()) {
            event.setCancelled(true);

            CraftingInventory inventory = event.getInventory();
            ItemStack[] matrix = inventory.getMatrix();

            int cores = countTotal(matrix, Material.HEAVY_CORE);
            int rods = countTotal(matrix, Material.BREEZE_ROD);
            int craftable = Math.min(cores, rods);

            int craftedMaces = Math.min(remaining, craftable);
            if (craftedMaces <= 0) {
                player.sendMessage(plugin.cfg().msg("limit-reached"));
                plugin.recipes().syncWithLimit();
                return;
            }

            consume(matrix, Material.HEAVY_CORE, craftedMaces);
            consume(matrix, Material.BREEZE_ROD, craftedMaces);
            inventory.setMatrix(matrix);

            for (int craftedIndex = 0; craftedIndex < craftedMaces; craftedIndex++) {
                ItemStack mace = plugin.registry().createAndRegisterNewMace(player, player.getLocation());
                giveOrDrop(player, mace);
            }

            broadcastCraft(player, craftedMaces, allowed);
            plugin.recipes().syncWithLimit();
            Bukkit.getScheduler().runTask(plugin, player::updateInventory);
            return;
        }

        ItemStack taggedMace = plugin.registry().createAndRegisterNewMace(player, player.getLocation());
        event.setCurrentItem(taggedMace);
        broadcastCraft(player, 1, allowed);
        plugin.recipes().syncWithLimit();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCrafter(CrafterCraftEvent event) {
        ItemStack recipeResult = event.getResult();
        if (!MaceItems.isMace(recipeResult)) return;

        event.setCancelled(true);
        event.setResult(new ItemStack(Material.AIR));
    }

    private int countTotal(ItemStack[] matrix, Material material) {
        int total = 0;
        for (ItemStack stack : matrix) {
            if (stack != null && stack.getType() == material) total += stack.getAmount();
        }
        return total;
    }

    private void consume(ItemStack[] matrix, Material material, int amount) {
        int remainingAmount = amount;
        for (int slot = 0; slot < matrix.length && remainingAmount > 0; slot++) {
            ItemStack stack = matrix[slot];
            if (stack == null || stack.getType() != material) continue;

            int consumedAmount = Math.min(remainingAmount, stack.getAmount());
            stack.setAmount(stack.getAmount() - consumedAmount);
            remainingAmount -= consumedAmount;

            if (stack.getAmount() <= 0) matrix[slot] = null;
        }
    }

    private void giveOrDrop(Player player, ItemStack stack) {
        HashMap<Integer, ItemStack> overflowStacks = player.getInventory().addItem(stack);
        if (!overflowStacks.isEmpty()) {
            overflowStacks.values().forEach(overflowStack -> player.getWorld().dropItemNaturally(player.getLocation(), overflowStack));
        }
    }

    private void broadcastCraft(Player player, int craftedMaces, int allowed) {
        Bukkit.broadcastMessage(plugin.cfg().msg("crafted-broadcast")
                .replace("%player%", player.getName())
                .replace("%amount%", String.valueOf(craftedMaces))
                .replace("%current%", String.valueOf(plugin.registry().getActiveCount()))
                .replace("%max%", String.valueOf(allowed)));
    }
}
