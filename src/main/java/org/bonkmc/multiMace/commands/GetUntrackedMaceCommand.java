package org.bonkmc.multiMace.commands;

import org.bonkmc.multiMace.MultiMace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class GetUntrackedMaceCommand implements CommandExecutor {
    private final MultiMace plugin;

    public GetUntrackedMaceCommand(MultiMace plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(plugin.cfg().msg("no-permission"));
            return true;
        }

        if (!sender.hasPermission("multimace.getuntracked")) {
            sender.sendMessage(plugin.cfg().msg("no-permission"));
            return true;
        }

        org.bukkit.inventory.ItemStack mace = plugin.registry().createAndRegisterNewMace(p, p.getLocation(), true);
        java.util.HashMap<Integer, org.bukkit.inventory.ItemStack> leftover = p.getInventory().addItem(mace);
        if (!leftover.isEmpty()) {
            leftover.values().forEach(it -> p.getWorld().dropItemNaturally(p.getLocation(), it));
        }

        sender.sendMessage(plugin.cfg().color("&aUntracked mace given! This mace bypasses the mace limit."));
        return true;
    }
}

