package org.bonkmc.limitedmaces.listeners;

import org.bonkmc.limitedmaces.LimitedMaces;
import org.bonkmc.limitedmaces.storage.MaceRecord;
import org.bukkit.Bukkit;

import java.util.UUID;

final class MaceDestructionNotifier {
    private final LimitedMaces plugin;

    MaceDestructionNotifier(LimitedMaces plugin) {
        this.plugin = plugin;
    }

    void broadcastDestroyed(UUID maceId, String reason) {
        MaceRecord record = plugin.registry().getRecord(maceId);
        if (record == null) {
            return;
        }

        if (record.isUntracked) {
            plugin.registry().removeTracked(maceId, reason);
            plugin.recipes().syncWithLimit();
            return;
        }

        String lastHolderName = record.lastHolderName != null && !record.lastHolderName.isBlank()
                ? record.lastHolderName
                : "Unknown";

        plugin.registry().removeTracked(maceId, reason);
        plugin.recipes().syncWithLimit();

        Bukkit.broadcastMessage(plugin.cfg().msg("destroyed-broadcast")
                .replace("%lastHolder%", lastHolderName)
                .replace("%reason%", reason)
                .replace("%current%", String.valueOf(plugin.registry().getActiveCount()))
                .replace("%max%", String.valueOf(plugin.cfg().getAllowedMaces())));
    }
}
