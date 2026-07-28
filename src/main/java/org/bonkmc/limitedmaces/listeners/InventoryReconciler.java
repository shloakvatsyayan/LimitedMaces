package org.bonkmc.limitedmaces.listeners;

import org.bonkmc.limitedmaces.LimitedMaces;
import org.bonkmc.limitedmaces.items.MaceItems;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

final class InventoryReconciler {
    private final LimitedMaces plugin;
    private final Set<UUID> pendingPlayerIds;
    private final Set<UUID> pendingMaceIds;
    private boolean hasPendingUntaggedMace;
    private boolean isReconciliationScheduled;

    InventoryReconciler(LimitedMaces plugin) {
        this.plugin = plugin;
        this.pendingPlayerIds = new LinkedHashSet<>();
        this.pendingMaceIds = new HashSet<>();
    }

    void enqueue(Player player, ItemStack... candidateStacks) {
        boolean hasMace = false;
        for (ItemStack stack : candidateStacks) {
            hasMace = rememberMace(stack) || hasMace;
        }
        if (!hasMace) {
            return;
        }

        pendingPlayerIds.add(player.getUniqueId());
        if (isReconciliationScheduled) {
            return;
        }

        isReconciliationScheduled = true;
        plugin.getServer().getScheduler().runTask(plugin, this::reconcilePending);
    }

    private boolean rememberMace(ItemStack stack) {
        if (!MaceItems.isMace(stack)) {
            return false;
        }

        Optional<UUID> maceId = plugin.registry().getTrackedId(stack);
        if (maceId.isPresent()) {
            pendingMaceIds.add(maceId.get());
        } else {
            hasPendingUntaggedMace = true;
        }
        return true;
    }

    private void reconcilePending() {
        Set<UUID> playerIds = new LinkedHashSet<>(pendingPlayerIds);
        Set<UUID> unresolvedMaceIds = new HashSet<>(pendingMaceIds);
        boolean shouldFindUntaggedMace = hasPendingUntaggedMace;
        clearPending();

        Set<UUID> checkedPlayerIds = new HashSet<>();
        for (UUID playerId : playerIds) {
            Player player = Bukkit.getPlayer(playerId);
            if (player == null || !player.isOnline()) {
                continue;
            }
            boolean foundUntaggedMace = inspectPlayer(
                    player,
                    unresolvedMaceIds,
                    shouldFindUntaggedMace,
                    true
            );
            shouldFindUntaggedMace = shouldFindUntaggedMace && !foundUntaggedMace;
            checkedPlayerIds.add(playerId);
        }

        if (unresolvedMaceIds.isEmpty() && !shouldFindUntaggedMace) {
            return;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!checkedPlayerIds.add(player.getUniqueId())) {
                continue;
            }
            boolean foundUntaggedMace = inspectPlayer(
                    player,
                    unresolvedMaceIds,
                    shouldFindUntaggedMace,
                    false
            );
            shouldFindUntaggedMace = shouldFindUntaggedMace && !foundUntaggedMace;
            if (unresolvedMaceIds.isEmpty() && !shouldFindUntaggedMace) {
                return;
            }
        }
    }

    private boolean inspectPlayer(
            Player player,
            Set<UUID> unresolvedMaceIds,
            boolean shouldFindUntaggedMace,
            boolean shouldAlwaysNormalize
    ) {
        boolean shouldNormalize = shouldAlwaysNormalize;
        boolean foundUntaggedMace = false;

        for (ItemStack stack : player.getInventory().getContents()) {
            InventoryInspectionMatch match = inspectStack(stack, unresolvedMaceIds, shouldFindUntaggedMace);
            shouldNormalize = shouldNormalize || match.hasMatch();
            foundUntaggedMace = foundUntaggedMace || match.hasUntaggedMace();
        }

        ItemStack cursorStack = player.getItemOnCursor();
        InventoryInspectionMatch cursorMatch = inspectStack(
                cursorStack,
                unresolvedMaceIds,
                shouldFindUntaggedMace
        );
        shouldNormalize = shouldNormalize || cursorMatch.hasMatch();
        foundUntaggedMace = foundUntaggedMace || cursorMatch.hasUntaggedMace();

        if (shouldNormalize) {
            plugin.registry().scanAndNormalizePlayerInventory(player);
            plugin.registry().getTrackedId(cursorStack)
                    .ifPresent(maceId -> plugin.registry().updateHeld(maceId, player));
        }
        return foundUntaggedMace;
    }

    private InventoryInspectionMatch inspectStack(
            ItemStack stack,
            Set<UUID> unresolvedMaceIds,
            boolean shouldFindUntaggedMace
    ) {
        if (!MaceItems.isMace(stack)) {
            return InventoryInspectionMatch.NONE;
        }

        Optional<UUID> maceId = plugin.registry().getTrackedId(stack);
        if (maceId.isPresent()) {
            return new InventoryInspectionMatch(unresolvedMaceIds.remove(maceId.get()), false);
        }
        return new InventoryInspectionMatch(shouldFindUntaggedMace, shouldFindUntaggedMace);
    }

    private void clearPending() {
        pendingPlayerIds.clear();
        pendingMaceIds.clear();
        hasPendingUntaggedMace = false;
        isReconciliationScheduled = false;
    }
}
