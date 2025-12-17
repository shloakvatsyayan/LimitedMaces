package org.bonkmc.multiMace.commands;

import org.bonkmc.multiMace.MultiMace;
import org.bonkmc.multiMace.storage.MaceRecord;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

public final class MacesCommand implements CommandExecutor, TabCompleter {
    private final MultiMace plugin;

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    public MacesCommand(MultiMace plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length >= 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("multimace.reload")) {
                sender.sendMessage(plugin.cfg().msg("no-permission"));
                return true;
            }

            plugin.cfg().reload();
            plugin.registry().load(); // reload tracked file too, keeps it consistent
            plugin.recipes().syncWithLimit();

            sender.sendMessage(plugin.cfg().msg("reload"));
            return true;
        }

        int allowed = plugin.cfg().getAllowedMaces();
        int active = plugin.registry().getActiveCount();
        int remaining = Math.max(0, allowed - active);

        sender.sendMessage(plugin.cfg().color("&eMaces: &f" + active + "&7/&f" + allowed + "  &7(remaining: " + remaining + ")"));

        List<MaceRecord> list = new ArrayList<>(plugin.registry().getAll());
        list.sort(Comparator.comparingLong(r -> -r.lastSeenAt));

        if (list.isEmpty()) {
            sender.sendMessage(plugin.cfg().color("&7(no tracked maces)"));
            return true;
        }

        int i = 1;
        for (MaceRecord r : list) {
            String holder = (r.lastHolderName != null && !r.lastHolderName.isBlank()) ? r.lastHolderName : "Unknown";
            String when = r.lastSeenAt > 0 ? FMT.format(Instant.ofEpochMilli(r.lastSeenAt)) : "unknown";
            String loc = (r.lastWorld == null || r.lastWorld.isBlank())
                    ? "unknown"
                    : (r.lastWorld + " " + (int) r.lastX + " " + (int) r.lastY + " " + (int) r.lastZ);

            String shortId = r.id.toString().split("-")[0];

            sender.sendMessage(plugin.cfg().color("&6#" + (i++) + " &e" + shortId +
                    " &7status=&f" + r.status +
                    " &7lastHolder=&f" + holder //+
                    //" &7lastSeen=&f" + when +
                    //" &7at=&f" + loc
                    )
            );
        }

        if (sender instanceof Player) {
            sender.sendMessage(plugin.cfg().color("&7Use &f/maces reload &7to reload config."));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> out = new ArrayList<>();
            if ("reload".startsWith(args[0].toLowerCase(Locale.ROOT))) out.add("reload");
            return out;
        }
        return Collections.emptyList();
    }
}
