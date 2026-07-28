package org.bonkmc.limitedmaces.listeners;

import org.bukkit.block.DoubleChest;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.BlockInventoryHolder;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.PlayerInventory;

final class ContainerStorageDetector {
    boolean isStorageInventory(InventoryView inventoryView) {
        if (inventoryView == null) {
            return false;
        }

        return isStorageInventory(inventoryView.getTopInventory(), inventoryView.getPlayer());
    }

    boolean isStorageInventory(Inventory inventory, HumanEntity viewer) {
        if (inventory == null) {
            return false;
        }

        boolean isPlayerInventory = inventory instanceof PlayerInventory
                || inventory.getType() == InventoryType.PLAYER;
        boolean isEnderChest = inventory.getType() == InventoryType.ENDER_CHEST;
        return isStorageInventory(inventory.getHolder(), viewer, isPlayerInventory, isEnderChest);
    }

    boolean isStorageInventory(
            InventoryHolder inventoryHolder,
            HumanEntity viewer,
            boolean isPlayerInventory,
            boolean isEnderChest
    ) {
        if (isPlayerInventory) {
            return false;
        }

        if (inventoryHolder instanceof HumanEntity inventoryOwner) {
            return viewer != null
                    && isEnderChest
                    && inventoryOwner.getUniqueId().equals(viewer.getUniqueId());
        }

        return inventoryHolder instanceof BlockInventoryHolder
                || inventoryHolder instanceof DoubleChest
                || inventoryHolder instanceof Entity;
    }
}
