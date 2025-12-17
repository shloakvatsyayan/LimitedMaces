package org.bonkmc.multiMace.commands;

import org.bonkmc.multiMace.MultiMace;
import org.bonkmc.multiMace.storage.MaceRecord;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public final class ClearUntrackedMacesCommand implements CommandExecutor {
    private final MultiMace plugin;

    public ClearUntrackedMacesCommand(MultiMace plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("multimace.clearuntracked")) {
            sender.sendMessage(plugin.cfg().msg("no-permission"));
            return true;
        }

        Collection<MaceRecord> untracked = plugin.registry().getUntrackedMaces();
        int count = untracked.size();

        for (Player p : Bukkit.getOnlinePlayers()) {
            org.bukkit.inventory.PlayerInventory inv = p.getInventory();
            ItemStack[] contents = inv.getContents();
            boolean changed = false;

            for (int i = 0; i < contents.length; i++) {
                ItemStack it = contents[i];
                if (it != null && it.getType() == Material.MACE) {
                    Optional<UUID> id = plugin.registry().getTrackedId(it);
                    if (id.isPresent()) {
                        MaceRecord record = plugin.registry().getRecord(id.get());
                        if (record != null && record.isUntracked) {
                            contents[i] = null;
                            changed = true;
                        }
                    }
                }
            }

            if (changed) {
                inv.setContents(contents);
            }

            ItemStack off = inv.getItemInOffHand();
            if (off != null && off.getType() == Material.MACE) {
                Optional<UUID> id = plugin.registry().getTrackedId(off);
                if (id.isPresent()) {
                    MaceRecord record = plugin.registry().getRecord(id.get());
                    if (record != null && record.isUntracked) {
                        inv.setItemInOffHand(null);
                    }
                }
            }
        }

        int droppedCount = 0;
        for (World world : Bukkit.getWorlds()) {
            for (org.bukkit.entity.Entity entity : world.getEntities()) {
                if (entity instanceof Item itemEntity) {
                    ItemStack itemStack = itemEntity.getItemStack();
                    if (itemStack != null && itemStack.getType() == Material.MACE) {
                        Optional<UUID> id = plugin.registry().getTrackedId(itemStack);
                        if (id.isPresent()) {
                            MaceRecord record = plugin.registry().getRecord(id.get());
                            if (record != null && record.isUntracked) {
                                itemEntity.remove();
                                droppedCount++;
                            }
                        }
                    }
                }
            }
        }

        plugin.registry().clearUntrackedMaces();

        sender.sendMessage(plugin.cfg().color("&aCleared &f" + count + " &auntracked mace(s) from inventories and &f" + droppedCount + " &adropped item(s)."));
        return true;
    }
}

