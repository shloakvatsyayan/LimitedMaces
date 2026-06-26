package org.bonkmc.limitedmaces.items;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public final class MaceItems {
    private MaceItems() {
    }

    public static boolean isMace(ItemStack itemStack) {
        return itemStack != null && itemStack.getType() == Material.MACE;
    }
}
