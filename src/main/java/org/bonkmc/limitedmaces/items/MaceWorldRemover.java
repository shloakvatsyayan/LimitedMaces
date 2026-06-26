package org.bonkmc.limitedmaces.items;

import org.bonkmc.limitedmaces.storage.MaceRecord;
import org.bonkmc.limitedmaces.storage.MaceRegistry;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

public final class MaceWorldRemover {
    private final MaceRegistry registry;
    private final MaceInventoryCleaner inventoryCleaner;

    public MaceWorldRemover(MaceRegistry registry) {
        this.registry = registry;
        this.inventoryCleaner = new MaceInventoryCleaner(registry);
    }

    public int removeMace(UUID maceId) {
        int removedMaces = 0;

        for (Player player : Bukkit.getOnlinePlayers()) {
            removedMaces += inventoryCleaner.removeMace(player.getInventory(), maceId);
        }

        return removedMaces + removeDroppedMaces(record -> maceId.equals(record.id));
    }

    public void removeUntrackedInventoryMaces() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            inventoryCleaner.removeUntrackedMaces(player.getInventory());
        }
    }

    public int removeUntrackedDroppedMaces() {
        return removeDroppedMaces(record -> record.isUntracked);
    }

    private int removeDroppedMaces(Predicate<MaceRecord> shouldRemoveRecord) {
        int removedMaces = 0;

        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (!(entity instanceof Item droppedMace)) {
                    continue;
                }

                ItemStack droppedStack = droppedMace.getItemStack();
                if (!MaceItems.isMace(droppedStack)) {
                    continue;
                }

                Optional<UUID> trackedMaceId = registry.getTrackedId(droppedStack);
                if (trackedMaceId.isEmpty()) {
                    continue;
                }

                MaceRecord record = registry.getRecord(trackedMaceId.get());
                if (record != null && shouldRemoveRecord.test(record)) {
                    droppedMace.remove();
                    removedMaces++;
                }
            }
        }

        return removedMaces;
    }
}
