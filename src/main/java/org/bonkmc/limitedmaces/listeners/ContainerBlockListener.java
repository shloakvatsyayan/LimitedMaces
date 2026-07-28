package org.bonkmc.limitedmaces.listeners;

import org.bonkmc.limitedmaces.LimitedMaces;
import org.bonkmc.limitedmaces.items.MaceItems;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

public final class ContainerBlockListener implements Listener {
    private final LimitedMaces plugin;
    private final ContainerInventoryRules rules;
    private final ContainerMaceReturner maceReturner;

    public ContainerBlockListener(LimitedMaces plugin) {
        this.plugin = plugin;
        this.rules = new ContainerInventoryRules(plugin);
        this.maceReturner = new ContainerMaceReturner();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        InventoryView inventoryView = event.getView();
        if (!rules.shouldBlockTop(inventoryView)) {
            return;
        }

        boolean isTopSlot = rules.isTopSlot(inventoryView, event.getRawSlot());
        ItemStack cursorStack = event.getCursor();
        ItemStack currentStack = event.getCurrentItem();

        if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY && MaceItems.isMace(currentStack) && !isTopSlot) {
            cancelContainerStorage(event, player);
            return;
        }

        if (isTopSlot && MaceItems.isMace(cursorStack)) {
            switch (event.getAction()) {
                case PLACE_ALL:
                case PLACE_ONE:
                case PLACE_SOME:
                case SWAP_WITH_CURSOR:
                case HOTBAR_SWAP:
                case HOTBAR_MOVE_AND_READD:
                    cancelContainerStorage(event, player);
                    return;
                default:
                    if (event.getClick() == ClickType.NUMBER_KEY || event.getClick() == ClickType.SWAP_OFFHAND) {
                        cancelContainerStorage(event, player);
                        return;
                    }
                    break;
            }
        }

        if (isTopSlot && (event.getAction() == InventoryAction.HOTBAR_SWAP || event.getClick() == ClickType.NUMBER_KEY)) {
            int hotbarButton = event.getHotbarButton();
            if (hotbarButton >= 0) {
                ItemStack hotbarStack = player.getInventory().getItem(hotbarButton);
                if (MaceItems.isMace(hotbarStack)) {
                    cancelContainerStorage(event, player);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        InventoryView inventoryView = event.getView();
        if (!rules.shouldBlockTop(inventoryView)) {
            return;
        }

        if (!MaceItems.isMace(event.getOldCursor())) {
            return;
        }

        int topSize = inventoryView.getTopInventory().getSize();
        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot < topSize) {
                cancelContainerStorage(event, player);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCreative(InventoryCreativeEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        InventoryView inventoryView = event.getView();
        ItemStack cursorStack = event.getCursor();

        if (MaceItems.isMace(cursorStack) && !rules.isTopSlot(inventoryView, event.getRawSlot())) {
            if (!plugin.registry().isTrackedMace(cursorStack)) {
                cancelContainerStorage(event, player);
                return;
            }
        }

        if (!rules.shouldBlockTop(inventoryView)) {
            return;
        }

        if (rules.isTopSlot(inventoryView, event.getRawSlot()) && MaceItems.isMace(event.getCursor())) {
            cancelContainerStorage(event, player);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPickup(InventoryPickupItemEvent event) {
        if (!plugin.cfg().isBlockContainerStorage()) {
            return;
        }

        if (MaceItems.isMace(event.getItem().getItemStack())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMove(InventoryMoveItemEvent event) {
        if (!plugin.cfg().isBlockContainerStorage()) {
            return;
        }

        if (MaceItems.isMace(event.getItem())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onOpen(InventoryOpenEvent event) {
        if (!plugin.cfg().isBlockContainerStorage()) {
            return;
        }

        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        InventoryView inventoryView = event.getView();
        if (!rules.shouldBlockTop(inventoryView)) {
            return;
        }

        Inventory topInventory = inventoryView.getTopInventory();
        if (maceReturner.returnStoredMaces(player, topInventory)) {
            player.sendMessage(plugin.cfg().msg("containers-blocked"));
            plugin.recipes().syncWithLimit();
        }
    }

    private void cancelContainerStorage(Cancellable event, Player player) {
        event.setCancelled(true);
        player.sendMessage(plugin.cfg().msg("containers-blocked"));
    }
}
