package org.bonkmc.limitedmaces.listeners;

import org.bonkmc.limitedmaces.LimitedMaces;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;

public final class InventoryTrackingListener implements Listener {
    private final ContainerStorageDetector storageDetector;
    private final InventoryReconciler inventoryReconciler;

    public InventoryTrackingListener(LimitedMaces plugin) {
        this.storageDetector = new ContainerStorageDetector();
        this.inventoryReconciler = new InventoryReconciler(plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)
                || event.getAction() == InventoryAction.NOTHING) {
            return;
        }

        ItemStack hotbarStack = null;
        int hotbarSlot = event.getHotbarButton();
        if (hotbarSlot >= 0) {
            hotbarStack = player.getInventory().getItem(hotbarSlot);
        }

        ItemStack offHandStack = null;
        if (event.getClick() == ClickType.SWAP_OFFHAND) {
            offHandStack = player.getInventory().getItemInOffHand();
        }

        inventoryReconciler.enqueue(
                player,
                event.getCurrentItem(),
                event.getCursor(),
                hotbarStack,
                offHandStack
        );
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            inventoryReconciler.enqueue(player, event.getOldCursor());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)
                || storageDetector.isStorageInventory(event.getView())) {
            return;
        }

        inventoryReconciler.enqueue(player, event.getView().getTopInventory().getContents());
    }
}
