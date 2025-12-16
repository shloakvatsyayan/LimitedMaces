package org.bonkmc.multiMace.listeners;

import org.bonkmc.multiMace.MultiMace;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView; // ✅ FIX: missing import
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;

public final class ContainerBlockListener implements Listener {
    private final MultiMace plugin;

    public ContainerBlockListener(MultiMace plugin) {
        this.plugin = plugin;
    }

    private boolean isMace(ItemStack it) {
        return it != null && it.getType() == Material.MACE;
    }

    private boolean isTopSlot(InventoryView view, int rawSlot) {
        return rawSlot >= 0 && rawSlot < view.getTopInventory().getSize();
    }

    /**
     * Treat ANY open top inventory as a blocked "container target" for maces.
     * This includes CHEST/BARREL/HOPPER/SHULKER, and also ANVIL + ENCHANTING, etc.
     */
    private boolean shouldBlockTop(InventoryView view) {
        return view != null && view.getTopInventory() != null && view.getTopInventory().getSize() > 0;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;

        InventoryView view = e.getView();
        if (!shouldBlockTop(view)) return;

        boolean inTop = isTopSlot(view, e.getRawSlot());

        ItemStack cursor = e.getCursor();
        ItemStack current = e.getCurrentItem();

        // 1) Shift-click: moving mace from player inventory INTO the top inventory
        if (e.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY && isMace(current) && !inTop) {
            e.setCancelled(true);
            p.sendMessage(plugin.cfg().msg("containers-blocked"));
            return;
        }

        // 2) Any placement of a mace (cursor) into a TOP slot (chest/barrel/hopper/anvil/enchant/etc)
        if (inTop && isMace(cursor)) {
            // Cancel any action that would place/swap the cursor item into the top slot
            switch (e.getAction()) {
                case PLACE_ALL:
                case PLACE_ONE:
                case PLACE_SOME:
                case SWAP_WITH_CURSOR:
                case HOTBAR_SWAP:
                case HOTBAR_MOVE_AND_READD: // if present in your API, harmless; if removed, just delete this line
                    e.setCancelled(true);
                    p.sendMessage(plugin.cfg().msg("containers-blocked"));
                    return;
                default:
                    // Also block common click-types that can still insert into slots
                    if (e.getClick() == ClickType.NUMBER_KEY || e.getClick() == ClickType.SWAP_OFFHAND) {
                        e.setCancelled(true);
                        p.sendMessage(plugin.cfg().msg("containers-blocked"));
                        return;
                    }
                    break;
            }
        }

        // 3) Number-key swap into TOP slot
        if (inTop && (e.getAction() == InventoryAction.HOTBAR_SWAP || e.getClick() == ClickType.NUMBER_KEY)) {
            int btn = e.getHotbarButton();
            if (btn >= 0) {
                ItemStack hot = p.getInventory().getItem(btn);
                if (isMace(hot)) {
                    e.setCancelled(true);
                    p.sendMessage(plugin.cfg().msg("containers-blocked"));
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDrag(InventoryDragEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;

        InventoryView view = e.getView();
        if (!shouldBlockTop(view)) return;

        if (!isMace(e.getOldCursor())) return;

        int topSize = view.getTopInventory().getSize();
        for (int rawSlot : e.getRawSlots()) {
            if (rawSlot < topSize) {
                e.setCancelled(true);
                p.sendMessage(plugin.cfg().msg("containers-blocked"));
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCreative(InventoryCreativeEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;

        InventoryView view = e.getView();
        if (!shouldBlockTop(view)) return;

        if (isTopSlot(view, e.getRawSlot()) && isMace(e.getCursor())) {
            e.setCancelled(true);
            p.sendMessage(plugin.cfg().msg("containers-blocked"));
        }
    }

    // Hoppers picking up dropped items (you said this works)
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPickup(InventoryPickupItemEvent e) {
        if (isMace(e.getItem().getItemStack())) e.setCancelled(true);
    }

    // Hopper transfers between inventories
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMove(InventoryMoveItemEvent e) {
        if (isMace(e.getItem())) e.setCancelled(true);
    }

    // If a mace somehow exists in a container/anvil/enchant/etc, strip it out when opened
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onOpen(InventoryOpenEvent e) {
        if (!(e.getPlayer() instanceof Player p)) return;

        Inventory top = e.getView().getTopInventory();
        if (top == null) return;

        boolean found = false;
        ItemStack[] contents = top.getContents();

        for (int i = 0; i < contents.length; i++) {
            ItemStack it = contents[i];
            if (!isMace(it)) continue;

            contents[i] = null;
            found = true;

            HashMap<Integer, ItemStack> leftover = p.getInventory().addItem(it);
            if (!leftover.isEmpty()) {
                leftover.values().forEach(drop -> p.getWorld().dropItemNaturally(p.getLocation(), drop));
            }
        }

        if (found) {
            top.setContents(contents);
            p.sendMessage(plugin.cfg().msg("containers-blocked"));
            plugin.registry().scanAndNormalizePlayerInventory(p);
            plugin.recipes().syncWithLimit();
        }
    }
}
