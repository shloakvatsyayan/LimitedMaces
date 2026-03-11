package org.bonkmc.limitedmaces.commands;

import org.bonkmc.limitedmaces.LimitedMaces;
import org.bonkmc.limitedmaces.storage.MaceRecord;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;

public final class MacesCommand implements CommandExecutor, TabCompleter {
    private final LimitedMaces plugin;

    public MacesCommand(LimitedMaces plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length >= 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("limitedmaces.reload")) {
                sender.sendMessage(plugin.cfg().msg("no-permission"));
                return true;
            }

            plugin.cfg().reload();
            plugin.registry().load();
            plugin.recipes().syncWithLimit();

            sender.sendMessage(plugin.cfg().msg("reload"));
            return true;
        }

        if (args.length >= 2 && args[0].equalsIgnoreCase("setlimit")) {
            if (!sender.hasPermission("limitedmaces.setlimit")) {
                sender.sendMessage(plugin.cfg().msg("no-permission"));
                return true;
            }

            try {
                int newLimit = Integer.parseInt(args[1]);
                if (newLimit < 0) {
                    sender.sendMessage(plugin.cfg().color("&cLimit must be 0 or greater."));
                    return true;
                }
                plugin.cfg().setAllowedMaces(newLimit);
                plugin.recipes().syncWithLimit();
                sender.sendMessage(plugin.cfg().color("&aMace limit set to &f" + newLimit + "&a."));
            } catch (NumberFormatException e) {
                sender.sendMessage(plugin.cfg().color("&cInvalid number: " + args[1]));
            }
            return true;
        }

        if (args.length >= 2 && args[0].equalsIgnoreCase("enchanting")) {
            if (!sender.hasPermission("limitedmaces.enchanting")) {
                sender.sendMessage(plugin.cfg().msg("no-permission"));
                return true;
            }

            String action = args[1].toLowerCase();
            if (action.equals("enable") || action.equals("on") || action.equals("true")) {
                plugin.cfg().setAllowMaceEnchanting(true);
                sender.sendMessage(plugin.cfg().color("&aMace enchanting &fenabled&a."));
            } else if (action.equals("disable") || action.equals("off") || action.equals("false")) {
                plugin.cfg().setAllowMaceEnchanting(false);
                sender.sendMessage(plugin.cfg().color("&aMace enchanting &fdisabled&a."));
            } else {
                sender.sendMessage(plugin.cfg().color("&cUsage: /maces enchanting <enable|disable>"));
            }
            return true;
        }

        int allowed = plugin.cfg().getAllowedMaces();
        int active = plugin.registry().getActiveCount();
        int remaining = Math.max(0, allowed - active);

        sender.sendMessage(plugin.cfg().color("&eMaces: &f" + active + "&7/&f" + allowed + "  &7(remaining: " + remaining + ")"));
        sender.sendMessage(plugin.cfg().color("&eEnchanting: &f" + (plugin.cfg().isAllowMaceEnchanting() ? "enabled" : "disabled")));

        List<MaceRecord> list = new ArrayList<>(plugin.registry().getAll());
        list.sort(Comparator.comparingLong(r -> -r.lastSeenAt));

        if (list.isEmpty()) {
            sender.sendMessage(plugin.cfg().color("&7(no tracked maces)"));
            return true;
        }

        int i = 1;
        for (MaceRecord r : list) {
            String holder = (r.lastHolderName != null && !r.lastHolderName.isBlank()) ? r.lastHolderName : "Unknown";
            String shortId = r.id.toString().split("-")[0];

            sender.sendMessage(plugin.cfg().color("&6#" + (i++) + " &e" + shortId +
                    " &7status=&f" + r.status +
                    " &7lastHolder=&f" + holder +
                    (r.isUntracked ? " &8[untracked]" : "")
                    )
            );
        }

        if (sender instanceof Player) {
            sender.sendMessage(plugin.cfg().color("&7Commands: &f/maces reload &7| &f/maces setlimit <n> &7| &f/maces enchanting <on|off>"));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> out = new ArrayList<>();
            String partial = args[0].toLowerCase(Locale.ROOT);
            if ("reload".startsWith(partial) && sender.hasPermission("limitedmaces.reload")) out.add("reload");
            if ("setlimit".startsWith(partial) && sender.hasPermission("limitedmaces.setlimit")) out.add("setlimit");
            if ("enchanting".startsWith(partial) && sender.hasPermission("limitedmaces.enchanting")) out.add("enchanting");
            return out;
        }
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("setlimit")) {
                return Arrays.asList("1", "2", "3", "5", "10");
            }
            if (args[0].equalsIgnoreCase("enchanting")) {
                List<String> out = new ArrayList<>();
                String partial = args[1].toLowerCase(Locale.ROOT);
                if ("enable".startsWith(partial)) out.add("enable");
                if ("disable".startsWith(partial)) out.add("disable");
                return out;
            }
        }
        return Collections.emptyList();
    }
}
