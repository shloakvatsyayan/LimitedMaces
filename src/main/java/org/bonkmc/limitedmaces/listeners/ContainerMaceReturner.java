package org.bonkmc.limitedmaces.listeners;

import org.bonkmc.limitedmaces.items.MaceItems;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;

final class ContainerMaceReturner {
    boolean returnStoredMaces(Player player, Inventory inventory) {
        boolean foundMace = false;
        ItemStack[] inventoryContents = inventory.getContents();

        for (int slot = 0; slot < inventoryContents.length; slot++) {
            ItemStack stack = inventoryContents[slot];
            if (!MaceItems.isMace(stack)) {
                continue;
            }

            inventoryContents[slot] = null;
            foundMace = true;

            HashMap<Integer, ItemStack> overflowStacks = player.getInventory().addItem(stack);
            if (!overflowStacks.isEmpty()) {
                overflowStacks.values().forEach(overflowStack -> player.getWorld().dropItemNaturally(player.getLocation(), overflowStack));
            }
        }

        if (foundMace) {
            inventory.setContents(inventoryContents);
        }

        return foundMace;
    }
}
