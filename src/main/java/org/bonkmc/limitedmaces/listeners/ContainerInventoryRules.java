package org.bonkmc.limitedmaces.listeners;

import org.bonkmc.limitedmaces.LimitedMaces;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;

import java.util.EnumSet;
import java.util.Set;

final class ContainerInventoryRules {
    private static final Set<Material> MACE_WORKBENCH_BLOCKS = EnumSet.of(
            Material.ANVIL,
            Material.CHIPPED_ANVIL,
            Material.DAMAGED_ANVIL,
            Material.ENCHANTING_TABLE,
            Material.GRINDSTONE
    );
    private static final Set<String> MACE_WORKBENCH_INVENTORIES = Set.of("ANVIL", "ENCHANTING", "GRINDSTONE");

    private final LimitedMaces plugin;
    private final ContainerStorageDetector storageDetector;

    ContainerInventoryRules(LimitedMaces plugin) {
        this.plugin = plugin;
        this.storageDetector = new ContainerStorageDetector();
    }

    boolean isTopSlot(InventoryView inventoryView, int rawSlot) {
        return rawSlot >= 0 && rawSlot < inventoryView.getTopInventory().getSize();
    }

    boolean shouldBlockTop(InventoryView inventoryView) {
        if (!plugin.cfg().isBlockContainerStorage()) {
            return false;
        }

        if (inventoryView == null || inventoryView.getTopInventory() == null) {
            return false;
        }

        Inventory topInventory = inventoryView.getTopInventory();
        if (topInventory.getSize() == 0) {
            return false;
        }

        if (isMaceWorkbench(topInventory)) {
            return !plugin.cfg().isAllowMaceEnchanting();
        }

        return storageDetector.isStorageInventory(inventoryView);
    }

    boolean isMaceWorkbench(Inventory inventory) {
        if (inventory == null) {
            return false;
        }

        InventoryHolder holder = inventory.getHolder();
        if (holder instanceof BlockState blockState && MACE_WORKBENCH_BLOCKS.contains(blockState.getType())) {
            return true;
        }

        if (holder instanceof Block block && MACE_WORKBENCH_BLOCKS.contains(block.getType())) {
            return true;
        }

        return MACE_WORKBENCH_INVENTORIES.contains(inventory.getType().name());
    }
}
