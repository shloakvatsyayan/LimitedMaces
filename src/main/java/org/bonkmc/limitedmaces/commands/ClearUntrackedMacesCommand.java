package org.bonkmc.limitedmaces.commands;

import org.bonkmc.limitedmaces.LimitedMaces;
import org.bonkmc.limitedmaces.items.MaceWorldRemover;
import org.bonkmc.limitedmaces.storage.MaceRecord;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.Collection;

public final class ClearUntrackedMacesCommand implements CommandExecutor {
    private final LimitedMaces plugin;
    private final MaceWorldRemover worldRemover;

    public ClearUntrackedMacesCommand(LimitedMaces plugin) {
        this.plugin = plugin;
        this.worldRemover = new MaceWorldRemover(plugin.registry());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("limitedmaces.clearuntracked")) {
            sender.sendMessage(plugin.cfg().msg("no-permission"));
            return true;
        }

        Collection<MaceRecord> untrackedMaces = plugin.registry().getUntrackedMaces();
        int untrackedCount = untrackedMaces.size();

        worldRemover.removeUntrackedInventoryMaces();
        int droppedCount = worldRemover.removeUntrackedDroppedMaces();

        plugin.registry().clearUntrackedMaces();

        sender.sendMessage(plugin.cfg().color("&aCleared &f" + untrackedCount + " &auntracked mace(s) from inventories and &f" + droppedCount + " &adropped item(s)."));
        return true;
    }
}
