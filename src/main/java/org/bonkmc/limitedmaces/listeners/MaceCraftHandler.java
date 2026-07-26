package org.bonkmc.limitedmaces.listeners;

import org.bonkmc.limitedmaces.LimitedMaces;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;

final class MaceCraftHandler {
    private final LimitedMaces plugin;

    MaceCraftHandler(LimitedMaces plugin) {
        this.plugin = plugin;
    }

    void craftIntoInventory(Player player, CraftingInventory inventory, int remaining, int allowed) {
        ItemStack[] matrix = inventory.getMatrix();
        int craftedMaces = getCraftableMaces(matrix, remaining);
        if (craftedMaces <= 0) {
            plugin.recipes().syncWithLimit();
            return;
        }

        consumeIngredients(matrix, craftedMaces);
        inventory.setMatrix(matrix);

        for (int craftedIndex = 0; craftedIndex < craftedMaces; craftedIndex++) {
            ItemStack mace = plugin.registry().createAndRegisterNewMace(player, player.getLocation());
            giveOrDrop(player, mace);
        }

        finishCraft(player, craftedMaces, allowed);
    }

    void craftSingle(Player player, CraftItemEvent event, int allowed) {
        if (!canDeliverSingleMace(player, event)) {
            Bukkit.getScheduler().runTask(plugin, player::updateInventory);
            return;
        }

        CraftingInventory inventory = event.getInventory();
        ItemStack[] matrix = inventory.getMatrix();
        if (getCraftableMaces(matrix, 1) <= 0) {
            plugin.recipes().syncWithLimit();
            Bukkit.getScheduler().runTask(plugin, player::updateInventory);
            return;
        }

        consumeIngredients(matrix, 1);
        inventory.setMatrix(matrix);

        ItemStack mace = plugin.registry().createAndRegisterNewMace(player, player.getLocation());
        deliverSingleMace(player, event, mace);
        finishCraft(player, 1, allowed);
    }

    private boolean canDeliverSingleMace(Player player, CraftItemEvent event) {
        ClickType clickType = event.getClick();
        return clickType == ClickType.DROP
                || clickType == ClickType.CONTROL_DROP
                || clickType == ClickType.NUMBER_KEY
                || isEmpty(player.getItemOnCursor());
    }

    private void deliverSingleMace(Player player, CraftItemEvent event, ItemStack mace) {
        ClickType clickType = event.getClick();
        if (clickType == ClickType.DROP || clickType == ClickType.CONTROL_DROP) {
            player.getWorld().dropItemNaturally(player.getLocation(), mace);
            return;
        }

        if (clickType == ClickType.NUMBER_KEY) {
            if (!placeInHotbar(player, event.getHotbarButton(), mace)) {
                giveOrDrop(player, mace);
            }
            return;
        }

        player.setItemOnCursor(mace);
    }

    private boolean placeInHotbar(Player player, int hotbarSlot, ItemStack mace) {
        if (hotbarSlot < 0 || hotbarSlot > 8) {
            return false;
        }

        ItemStack existingStack = player.getInventory().getItem(hotbarSlot);
        if (!isEmpty(existingStack)) {
            return false;
        }

        player.getInventory().setItem(hotbarSlot, mace);
        return true;
    }

    private int getCraftableMaces(ItemStack[] matrix, int remaining) {
        int cores = countTotal(matrix, Material.HEAVY_CORE);
        int rods = countTotal(matrix, Material.BREEZE_ROD);
        return Math.min(remaining, Math.min(cores, rods));
    }

    private void consumeIngredients(ItemStack[] matrix, int craftedMaces) {
        consume(matrix, Material.HEAVY_CORE, craftedMaces);
        consume(matrix, Material.BREEZE_ROD, craftedMaces);
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

    private boolean isEmpty(ItemStack stack) {
        return stack == null || stack.getType() == Material.AIR;
    }

    private void finishCraft(Player player, int craftedMaces, int allowed) {
        broadcastCraft(player, craftedMaces, allowed);
        plugin.recipes().syncWithLimit();
        Bukkit.getScheduler().runTask(plugin, player::updateInventory);
    }

    private void broadcastCraft(Player player, int craftedMaces, int allowed) {
        Bukkit.broadcastMessage(plugin.cfg().msg("crafted-broadcast")
                .replace("%player%", player.getName())
                .replace("%amount%", String.valueOf(craftedMaces))
                .replace("%current%", String.valueOf(plugin.registry().getActiveCount()))
                .replace("%max%", String.valueOf(allowed)));
    }
}
