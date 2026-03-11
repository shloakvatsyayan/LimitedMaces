package org.bonkmc.limitedmaces.commands;

import org.bonkmc.limitedmaces.LimitedMaces;
import org.bonkmc.limitedmaces.storage.MaceRecord;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.*;
import java.util.stream.Collectors;

public final class RemoveMaceCommand implements CommandExecutor, TabCompleter {
    private final LimitedMaces plugin;

    public RemoveMaceCommand(LimitedMaces plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("limitedmaces.remove")) {
            sender.sendMessage(plugin.cfg().msg("no-permission"));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(plugin.cfg().color("&cUsage: /removemace <mace-id>"));
            sender.sendMessage(plugin.cfg().color("&7Use /maces to see mace IDs."));
            return true;
        }

        String partialId = args[0].toLowerCase();
        
        MaceRecord targetRecord = null;
        for (MaceRecord record : plugin.registry().getAll()) {
            String fullId = record.id.toString().toLowerCase();
            String shortId = fullId.split("-")[0];
            if (fullId.equals(partialId) || shortId.equals(partialId) || fullId.startsWith(partialId)) {
                targetRecord = record;
                break;
            }
        }

        if (targetRecord == null) {
            sender.sendMessage(plugin.cfg().color("&cNo mace found with ID: " + partialId));
            return true;
        }

        UUID maceId = targetRecord.id;
        int removed = removeMaceFromWorld(maceId);

        plugin.registry().removeTracked(maceId, "admin-removed");
        plugin.recipes().syncWithLimit();

        sender.sendMessage(plugin.cfg().color("&aMace &f" + maceId.toString().split("-")[0] + " &aremoved from the plugin. Removed &f" + removed + " &aitem(s) from the world."));
        return true;
    }

    private int removeMaceFromWorld(UUID maceId) {
        int count = 0;

        for (Player p : Bukkit.getOnlinePlayers()) {
            count += removeFromInventory(p.getInventory(), maceId);
        }

        for (World world : Bukkit.getWorlds()) {
            for (org.bukkit.entity.Entity entity : world.getEntities()) {
                if (entity instanceof Item itemEntity) {
                    ItemStack itemStack = itemEntity.getItemStack();
                    if (itemStack != null && itemStack.getType() == Material.MACE) {
                        Optional<UUID> id = plugin.registry().getTrackedId(itemStack);
                        if (id.isPresent() && id.get().equals(maceId)) {
                            itemEntity.remove();
                            count++;
                        }
                    }
                }
            }
        }

        return count;
    }

    private int removeFromInventory(PlayerInventory inv, UUID maceId) {
        int count = 0;
        
        ItemStack[] contents = inv.getContents();
        boolean changed = false;
        
        for (int i = 0; i < contents.length; i++) {
            ItemStack it = contents[i];
            if (it != null && it.getType() == Material.MACE) {
                Optional<UUID> id = plugin.registry().getTrackedId(it);
                if (id.isPresent() && id.get().equals(maceId)) {
                    contents[i] = null;
                    changed = true;
                    count++;
                }
            }
        }
        
        if (changed) {
            inv.setContents(contents);
        }

        ItemStack off = inv.getItemInOffHand();
        if (off != null && off.getType() == Material.MACE) {
            Optional<UUID> id = plugin.registry().getTrackedId(off);
            if (id.isPresent() && id.get().equals(maceId)) {
                inv.setItemInOffHand(null);
                count++;
            }
        }
        
        return count;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            return plugin.registry().getAll().stream()
                    .map(r -> r.id.toString().split("-")[0])
                    .filter(id -> id.toLowerCase().startsWith(partial))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
