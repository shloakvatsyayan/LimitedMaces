package org.bonkmc.limitedmaces.items;

import org.bonkmc.limitedmaces.storage.MaceRecord;
import org.bonkmc.limitedmaces.storage.MaceRegistry;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

public final class MaceInventoryCleaner {
    private final MaceRegistry registry;

    public MaceInventoryCleaner(MaceRegistry registry) {
        this.registry = registry;
    }

    public int removeMace(PlayerInventory inventory, UUID maceId) {
        int removedMaces = removeMatchingFromContents(inventory, stack -> hasTrackedId(stack, maceId));
        if (removeMatchingOffHand(inventory, stack -> hasTrackedId(stack, maceId))) {
            removedMaces++;
        }
        return removedMaces;
    }

    public boolean removeFirstMace(PlayerInventory inventory, UUID maceId) {
        if (removeFirstFromContents(inventory, stack -> hasTrackedId(stack, maceId))) {
            return true;
        }
        return removeMatchingOffHand(inventory, stack -> hasTrackedId(stack, maceId));
    }

    public boolean removeFirstUntaggedMace(PlayerInventory inventory) {
        if (removeFirstFromContents(inventory, this::isUntaggedMace)) {
            return true;
        }
        return removeMatchingOffHand(inventory, this::isUntaggedMace);
    }

    public boolean removeUntrackedMaces(PlayerInventory inventory) {
        int removedMaces = removeMatchingFromContents(inventory, this::isUntrackedMace);
        return removeMatchingOffHand(inventory, this::isUntrackedMace) || removedMaces > 0;
    }

    private int removeMatchingFromContents(PlayerInventory inventory, Predicate<ItemStack> shouldRemoveStack) {
        ItemStack[] inventoryContents = inventory.getContents();
        int removedMaces = 0;
        boolean hasChanges = false;

        for (int slot = 0; slot < inventoryContents.length; slot++) {
            ItemStack stack = inventoryContents[slot];
            if (shouldRemoveStack.test(stack)) {
                inventoryContents[slot] = null;
                removedMaces++;
                hasChanges = true;
            }
        }

        if (hasChanges) {
            inventory.setContents(inventoryContents);
        }

        return removedMaces;
    }

    private boolean removeFirstFromContents(PlayerInventory inventory, Predicate<ItemStack> shouldRemoveStack) {
        ItemStack[] inventoryContents = inventory.getContents();

        for (int slot = 0; slot < inventoryContents.length; slot++) {
            ItemStack stack = inventoryContents[slot];
            if (shouldRemoveStack.test(stack)) {
                inventoryContents[slot] = null;
                inventory.setContents(inventoryContents);
                return true;
            }
        }

        return false;
    }

    private boolean removeMatchingOffHand(PlayerInventory inventory, Predicate<ItemStack> shouldRemoveStack) {
        ItemStack offHandStack = inventory.getItemInOffHand();
        if (!shouldRemoveStack.test(offHandStack)) {
            return false;
        }

        inventory.setItemInOffHand(null);
        return true;
    }

    private boolean hasTrackedId(ItemStack stack, UUID maceId) {
        Optional<UUID> trackedMaceId = registry.getTrackedId(stack);
        return trackedMaceId.isPresent() && trackedMaceId.get().equals(maceId);
    }

    private boolean isUntaggedMace(ItemStack stack) {
        return MaceItems.isMace(stack) && registry.getTrackedId(stack).isEmpty();
    }

    private boolean isUntrackedMace(ItemStack stack) {
        Optional<UUID> trackedMaceId = registry.getTrackedId(stack);
        if (trackedMaceId.isEmpty()) {
            return false;
        }

        MaceRecord record = registry.getRecord(trackedMaceId.get());
        return record != null && record.isUntracked;
    }
}
