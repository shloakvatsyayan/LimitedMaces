package org.bonkmc.limitedmaces.storage;

import org.bonkmc.limitedmaces.LimitedMaces;
import org.bonkmc.limitedmaces.items.MaceInventoryCleaner;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

final class MaceInventoryNormalizer {
    private final LimitedMaces plugin;
    private final MaceRegistry registry;
    private final MaceInventoryCleaner inventoryCleaner;

    MaceInventoryNormalizer(LimitedMaces plugin, MaceRegistry registry) {
        this.plugin = plugin;
        this.registry = registry;
        this.inventoryCleaner = new MaceInventoryCleaner(registry);
    }

    void scanAndNormalize(Player player) {
        if (player == null) {
            return;
        }

        PlayerInventory inventory = player.getInventory();
        List<ItemStack> inventoryStacks = new ArrayList<>();
        inventoryStacks.addAll(Arrays.asList(inventory.getStorageContents()));
        inventoryStacks.addAll(Arrays.asList(inventory.getArmorContents()));
        inventoryStacks.addAll(Arrays.asList(inventory.getExtraContents()));

        Set<UUID> seenMaceIds = new HashSet<>();
        for (ItemStack stack : inventoryStacks) {
            if (!registry.isMace(stack)) {
                continue;
            }

            Optional<UUID> trackedMaceId = registry.getTrackedId(stack);
            if (trackedMaceId.isPresent()) {
                normalizeTaggedMace(player, trackedMaceId.get(), seenMaceIds);
            } else {
                normalizeUntaggedMace(player, stack);
            }
        }
    }

    private void normalizeTaggedMace(Player player, UUID maceId, Set<UUID> seenMaceIds) {
        if (seenMaceIds.contains(maceId)) {
            inventoryCleaner.removeFirstMace(player.getInventory(), maceId);
            player.sendMessage(plugin.cfg().msg("illegal-removed"));
            return;
        }

        seenMaceIds.add(maceId);
        MaceRecord record = registry.getRecord(maceId);
        if (record == null) {
            registerMissingTaggedMace(player, maceId);
            return;
        }

        registry.updateHeld(maceId, player);
    }

    private void registerMissingTaggedMace(Player player, UUID maceId) {
        if (registry.getActiveCount() < plugin.cfg().getAllowedMaces()) {
            registry.ensureRegisteredExisting(maceId, player, player.getLocation(), "HELD");
            return;
        }

        inventoryCleaner.removeFirstMace(player.getInventory(), maceId);
        player.sendMessage(plugin.cfg().msg("illegal-removed"));
    }

    private void normalizeUntaggedMace(Player player, ItemStack maceStack) {
        if (registry.getActiveCount() < plugin.cfg().getAllowedMaces()) {
            UUID maceId = UUID.randomUUID();
            registry.tagWithId(maceStack, maceId);
            registry.ensureRegisteredExisting(maceId, player, player.getLocation(), "HELD");
            return;
        }

        inventoryCleaner.removeFirstUntaggedMace(player.getInventory());
        player.sendMessage(plugin.cfg().msg("illegal-removed"));
    }
}
