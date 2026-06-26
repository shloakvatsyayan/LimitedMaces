package org.bonkmc.limitedmaces.commands;

import org.bonkmc.limitedmaces.LimitedMaces;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;

public final class GetUntrackedMaceCommand implements CommandExecutor {
    private final LimitedMaces plugin;

    public GetUntrackedMaceCommand(LimitedMaces plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.cfg().msg("no-permission"));
            return true;
        }

        if (!sender.hasPermission("limitedmaces.getuntracked")) {
            sender.sendMessage(plugin.cfg().msg("no-permission"));
            return true;
        }

        ItemStack mace = plugin.registry().createAndRegisterNewMace(player, player.getLocation(), true);
        HashMap<Integer, ItemStack> overflowStacks = player.getInventory().addItem(mace);
        if (!overflowStacks.isEmpty()) {
            overflowStacks.values().forEach(stack -> player.getWorld().dropItemNaturally(player.getLocation(), stack));
        }

        sender.sendMessage(plugin.cfg().color("&aUntracked mace given! This mace bypasses the mace limit."));
        return true;
    }
}
