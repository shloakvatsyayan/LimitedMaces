package org.bonkmc.multiMace.listeners;

import org.bonkmc.multiMace.MultiMace;
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
    private final MultiMace plugin;

    public CraftingListener(MultiMace plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepare(PrepareItemCraftEvent e) {
        if (e.getRecipe() == null) return;
        ItemStack res = e.getRecipe().getResult();
        if (res == null || res.getType() != Material.MACE) return;

        if (plugin.registry().getActiveCount() >= plugin.cfg().getAllowedMaces()) {
            e.getInventory().setResult(new ItemStack(Material.AIR));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCraft(CraftItemEvent e) {
        if (e.getRecipe() == null) return;
        ItemStack res = e.getRecipe().getResult();
        if (res == null || res.getType() != Material.MACE) return;

        if (!(e.getWhoClicked() instanceof Player p)) return;

        int allowed = plugin.cfg().getAllowedMaces();
        int active = plugin.registry().getActiveCount();
        int remaining = Math.max(0, allowed - active);

        if (remaining <= 0) {
            e.setCancelled(true);
            p.sendMessage(plugin.cfg().msg("limit-reached"));
            plugin.recipes().syncWithLimit();
            return;
        }

        if (e.isShiftClick()) {
            e.setCancelled(true);

            CraftingInventory inv = e.getInventory();
            ItemStack[] matrix = inv.getMatrix();

            int cores = countTotal(matrix, Material.HEAVY_CORE);
            int rods  = countTotal(matrix, Material.BREEZE_ROD);
            int craftable = Math.min(cores, rods);

            int toMake = Math.min(remaining, craftable);
            if (toMake <= 0) {
                p.sendMessage(plugin.cfg().msg("limit-reached"));
                plugin.recipes().syncWithLimit();
                return;
            }

            consume(matrix, Material.HEAVY_CORE, toMake);
            consume(matrix, Material.BREEZE_ROD, toMake);
            inv.setMatrix(matrix);

            for (int i = 0; i < toMake; i++) {
                ItemStack mace = plugin.registry().createAndRegisterNewMace(p, p.getLocation());
                HashMap<Integer, ItemStack> leftover = p.getInventory().addItem(mace);
                if (!leftover.isEmpty()) {
                    leftover.values().forEach(it -> p.getWorld().dropItemNaturally(p.getLocation(), it));
                }
            }

            Bukkit.broadcastMessage(plugin.cfg().msg("crafted-broadcast")
                    .replace("%player%", p.getName())
                    .replace("%amount%", String.valueOf(toMake))
                    .replace("%current%", String.valueOf(plugin.registry().getActiveCount()))
                    .replace("%max%", String.valueOf(allowed)));

            plugin.recipes().syncWithLimit();
            Bukkit.getScheduler().runTask(plugin, p::updateInventory);
            return;
        }

        ItemStack tagged = plugin.registry().createAndRegisterNewMace(p, p.getLocation());
        e.setCurrentItem(tagged);

        Bukkit.broadcastMessage(plugin.cfg().msg("crafted-broadcast")
                .replace("%player%", p.getName())
                .replace("%amount%", "1")
                .replace("%current%", String.valueOf(plugin.registry().getActiveCount()))
                .replace("%max%", String.valueOf(allowed)));

        plugin.recipes().syncWithLimit();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCrafter(CrafterCraftEvent e) {
        ItemStack res = e.getResult();
        if (res == null || res.getType() != Material.MACE) return;

        e.setCancelled(true);
        e.setResult(new ItemStack(Material.AIR));
    }

    private int countTotal(ItemStack[] matrix, Material m) {
        int total = 0;
        for (ItemStack it : matrix) {
            if (it != null && it.getType() == m) total += it.getAmount();
        }
        return total;
    }

    private void consume(ItemStack[] matrix, Material m, int amount) {
        int left = amount;
        for (int i = 0; i < matrix.length && left > 0; i++) {
            ItemStack it = matrix[i];
            if (it == null || it.getType() != m) continue;

            int take = Math.min(left, it.getAmount());
            it.setAmount(it.getAmount() - take);
            left -= take;

            if (it.getAmount() <= 0) matrix[i] = null;
        }
    }
}
