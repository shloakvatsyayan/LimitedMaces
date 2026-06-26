package org.bonkmc.limitedmaces.commands;

import org.bonkmc.limitedmaces.LimitedMaces;
import org.bonkmc.limitedmaces.storage.MaceRecord;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class MacesCommand implements CommandExecutor, TabCompleter {
    private final LimitedMaces plugin;

    public MacesCommand(LimitedMaces plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length >= 1 && args[0].equalsIgnoreCase("reload")) {
            reloadPlugin(sender);
            return true;
        }

        if (args.length >= 2 && args[0].equalsIgnoreCase("setlimit")) {
            setLimit(sender, args[1]);
            return true;
        }

        if (args.length >= 2 && args[0].equalsIgnoreCase("enchanting")) {
            setEnchanting(sender, args[1]);
            return true;
        }

        sendStatus(sender);
        return true;
    }

    private void reloadPlugin(CommandSender sender) {
        if (!sender.hasPermission("limitedmaces.reload")) {
            sender.sendMessage(plugin.cfg().msg("no-permission"));
            return;
        }

        plugin.cfg().reload();
        plugin.registry().load();
        plugin.recipes().syncWithLimit();
        sender.sendMessage(plugin.cfg().msg("reload"));
    }

    private void setLimit(CommandSender sender, String rawLimit) {
        if (!sender.hasPermission("limitedmaces.setlimit")) {
            sender.sendMessage(plugin.cfg().msg("no-permission"));
            return;
        }

        try {
            int newLimit = Integer.parseInt(rawLimit);
            if (newLimit < 0) {
                sender.sendMessage(plugin.cfg().color("&cLimit must be 0 or greater."));
                return;
            }
            plugin.cfg().setAllowedMaces(newLimit);
            plugin.recipes().syncWithLimit();
            sender.sendMessage(plugin.cfg().color("&aMace limit set to &f" + newLimit + "&a."));
        } catch (NumberFormatException exception) {
            sender.sendMessage(plugin.cfg().color("&cInvalid number: " + rawLimit));
        }
    }

    private void setEnchanting(CommandSender sender, String rawAction) {
        if (!sender.hasPermission("limitedmaces.enchanting")) {
            sender.sendMessage(plugin.cfg().msg("no-permission"));
            return;
        }

        String action = rawAction.toLowerCase(Locale.ROOT);
        if (action.equals("enable") || action.equals("on") || action.equals("true")) {
            plugin.cfg().setAllowMaceEnchanting(true);
            sender.sendMessage(plugin.cfg().color("&aMace enchanting &fenabled&a."));
            return;
        }

        if (action.equals("disable") || action.equals("off") || action.equals("false")) {
            plugin.cfg().setAllowMaceEnchanting(false);
            sender.sendMessage(plugin.cfg().color("&aMace enchanting &fdisabled&a."));
            return;
        }

        sender.sendMessage(plugin.cfg().color("&cUsage: /maces enchanting <enable|disable>"));
    }

    private void sendStatus(CommandSender sender) {
        int allowed = plugin.cfg().getAllowedMaces();
        int active = plugin.registry().getActiveCount();
        int remaining = Math.max(0, allowed - active);

        sender.sendMessage(plugin.cfg().color("&eMaces: &f" + active + "&7/&f" + allowed + "  &7(remaining: " + remaining + ")"));
        sender.sendMessage(plugin.cfg().color("&eEnchanting: &f" + (plugin.cfg().isAllowMaceEnchanting() ? "enabled" : "disabled")));

        List<MaceRecord> records = new ArrayList<>(plugin.registry().getAll());
        records.sort(Comparator.comparingLong(record -> -record.lastSeenAt));

        if (records.isEmpty()) {
            sender.sendMessage(plugin.cfg().color("&7(no tracked maces)"));
            return;
        }

        int position = 1;
        for (MaceRecord record : records) {
            String holderName = record.lastHolderName != null && !record.lastHolderName.isBlank() ? record.lastHolderName : "Unknown";
            String shortMaceId = record.id.toString().split("-")[0];

            sender.sendMessage(plugin.cfg().color("&6#" + (position++) + " &e" + shortMaceId +
                    " &7status=&f" + record.status +
                    " &7lastHolder=&f" + holderName +
                    (record.isUntracked ? " &8[untracked]" : "")
                    )
            );
        }

        if (sender instanceof Player) {
            sender.sendMessage(plugin.cfg().color("&7Commands: &f/maces reload &7| &f/maces setlimit <n> &7| &f/maces enchanting <on|off>"));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            String partial = args[0].toLowerCase(Locale.ROOT);
            if ("reload".startsWith(partial) && sender.hasPermission("limitedmaces.reload")) completions.add("reload");
            if ("setlimit".startsWith(partial) && sender.hasPermission("limitedmaces.setlimit")) completions.add("setlimit");
            if ("enchanting".startsWith(partial) && sender.hasPermission("limitedmaces.enchanting")) completions.add("enchanting");
            return completions;
        }
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("setlimit")) {
                return Arrays.asList("1", "2", "3", "5", "10");
            }
            if (args[0].equalsIgnoreCase("enchanting")) {
                List<String> completions = new ArrayList<>();
                String partial = args[1].toLowerCase(Locale.ROOT);
                if ("enable".startsWith(partial)) completions.add("enable");
                if ("disable".startsWith(partial)) completions.add("disable");
                return completions;
            }
        }
        return Collections.emptyList();
    }
}
