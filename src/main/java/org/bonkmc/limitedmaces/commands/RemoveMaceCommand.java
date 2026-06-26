package org.bonkmc.limitedmaces.commands;

import org.bonkmc.limitedmaces.LimitedMaces;
import org.bonkmc.limitedmaces.items.MaceWorldRemover;
import org.bonkmc.limitedmaces.storage.MaceRecord;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

public final class RemoveMaceCommand implements CommandExecutor, TabCompleter {
    private final LimitedMaces plugin;
    private final MaceWorldRemover worldRemover;

    public RemoveMaceCommand(LimitedMaces plugin) {
        this.plugin = plugin;
        this.worldRemover = new MaceWorldRemover(plugin.registry());
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

        String partialMaceId = args[0].toLowerCase(Locale.ROOT);
        MaceRecord targetRecord = findRecord(partialMaceId, plugin.registry().getAll());

        if (targetRecord == null) {
            sender.sendMessage(plugin.cfg().color("&cNo mace found with ID: " + partialMaceId));
            return true;
        }

        UUID maceId = targetRecord.id;
        int removedMaces = worldRemover.removeMace(maceId);

        plugin.registry().removeTracked(maceId, "admin-removed");
        plugin.recipes().syncWithLimit();

        sender.sendMessage(plugin.cfg().color("&aMace &f" + shortId(maceId) + " &aremoved from the plugin. Removed &f" + removedMaces + " &aitem(s) from the world."));
        return true;
    }

    private MaceRecord findRecord(String partialMaceId, Collection<MaceRecord> records) {
        for (MaceRecord record : records) {
            String fullMaceId = record.id.toString().toLowerCase(Locale.ROOT);
            if (matchesMaceId(fullMaceId, partialMaceId)) {
                return record;
            }
        }
        return null;
    }

    private boolean matchesMaceId(String fullMaceId, String partialMaceId) {
        String shortMaceId = fullMaceId.split("-")[0];
        return fullMaceId.equals(partialMaceId) || shortMaceId.equals(partialMaceId) || fullMaceId.startsWith(partialMaceId);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String partialMaceId = args[0].toLowerCase(Locale.ROOT);
            return plugin.registry().getAll().stream()
                    .map(record -> shortId(record.id))
                    .filter(maceId -> maceId.toLowerCase(Locale.ROOT).startsWith(partialMaceId))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    private String shortId(UUID maceId) {
        return maceId.toString().split("-")[0];
    }
}
