package org.bonkmc.multiMace.listeners;

import org.bonkmc.multiMace.MultiMace;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;
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

    private boolean shouldBlockTop(InventoryView view) {
        if (view == null || view.getTopInventory() == null) {
            return false;
        }

        Inventory top = view.getTopInventory();
        if (top.getSize() == 0) {
            return false;
        }

        if (plugin.cfg().isAllowMaceEnchanting()) {
            if (isAnvilOrEnchantmentTable(top)) {
                return false;
            }
        }

        return true;
    }

    private boolean isAnvilOrEnchantmentTable(Inventory inventory) {
        if (inventory == null) return false;
        
        InventoryHolder holder = inventory.getHolder();
        if (holder instanceof BlockState blockState) {
            Material blockType = blockState.getType();
            if (blockType == Material.ANVIL || 
                blockType == Material.CHIPPED_ANVIL || 
                blockType == Material.DAMAGED_ANVIL ||
                blockType == Material.ENCHANTING_TABLE ||
                blockType == Material.GRINDSTONE) {
                return true;
            }
        }
        
        try {
            String typeName = inventory.getType().name();
            if (typeName.equals("ANVIL") || typeName.equals("ENCHANTING") || typeName.equals("GRINDSTONE")) {
                return true;
            }
        } catch (Exception ignored) {
        }
        
        if (holder instanceof Block block) {
            Material blockType = block.getType();
            if (blockType == Material.ANVIL || 
                blockType == Material.CHIPPED_ANVIL || 
                blockType == Material.DAMAGED_ANVIL ||
                blockType == Material.ENCHANTING_TABLE ||
                blockType == Material.GRINDSTONE) {
                return true;
            }
        }
        
        return false;
    }
    
    private boolean isAnvilOrEnchantmentTable(InventoryView view) {
        if (view == null) return false;
        Inventory top = view.getTopInventory();
        return top != null && isAnvilOrEnchantmentTable(top);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;

        InventoryView view = e.getView();
        
        if (plugin.cfg().isAllowMaceEnchanting() && isAnvilOrEnchantmentTable(view)) {
            return;
        }
        
        if (!shouldBlockTop(view)) return;

        boolean inTop = isTopSlot(view, e.getRawSlot());

        ItemStack cursor = e.getCursor();
        ItemStack current = e.getCurrentItem();

        if (e.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY && isMace(current) && !inTop) {
            e.setCancelled(true);
            p.sendMessage(plugin.cfg().msg("containers-blocked"));
            return;
        }

        if (inTop && isMace(cursor)) {
            switch (e.getAction()) {
                case PLACE_ALL:
                case PLACE_ONE:
                case PLACE_SOME:
                case SWAP_WITH_CURSOR:
                case HOTBAR_SWAP:
                case HOTBAR_MOVE_AND_READD:
                    e.setCancelled(true);
                    p.sendMessage(plugin.cfg().msg("containers-blocked"));
                    return;
                default:
                    if (e.getClick() == ClickType.NUMBER_KEY || e.getClick() == ClickType.SWAP_OFFHAND) {
                        e.setCancelled(true);
                        p.sendMessage(plugin.cfg().msg("containers-blocked"));
                        return;
                    }
                    break;
            }
        }

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
        
        if (plugin.cfg().isAllowMaceEnchanting() && isAnvilOrEnchantmentTable(view)) {
            return;
        }
        
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
        
        ItemStack cursor = e.getCursor();
        
        if (isMace(cursor) && !isTopSlot(view, e.getRawSlot())) {
            if (!plugin.registry().isTrackedMace(cursor)) {
                e.setCancelled(true);
                p.sendMessage(plugin.cfg().msg("containers-blocked"));
                return;
            }
        }
        
        if (plugin.cfg().isAllowMaceEnchanting() && isAnvilOrEnchantmentTable(view)) {
            return;
        }
        
        if (!shouldBlockTop(view)) return;

        if (isTopSlot(view, e.getRawSlot()) && isMace(e.getCursor())) {
            e.setCancelled(true);
            p.sendMessage(plugin.cfg().msg("containers-blocked"));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPickup(InventoryPickupItemEvent e) {
        if (isMace(e.getItem().getItemStack())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMove(InventoryMoveItemEvent e) {
        if (isMace(e.getItem())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onOpen(InventoryOpenEvent e) {
        if (!(e.getPlayer() instanceof Player p)) return;

        Inventory top = e.getView().getTopInventory();
        if (top == null) return;

        if (plugin.cfg().isAllowMaceEnchanting()) {
            if (isAnvilOrEnchantmentTable(top)) {
                return;
            }
        }

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
            plugin.recipes().syncWithLimit();
        }
    }
}
