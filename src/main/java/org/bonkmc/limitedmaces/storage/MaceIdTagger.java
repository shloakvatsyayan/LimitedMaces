package org.bonkmc.limitedmaces.storage;

import org.bonkmc.limitedmaces.items.MaceItems;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;
import java.util.UUID;

final class MaceIdTagger {
    private final JavaPlugin plugin;
    private final NamespacedKey maceIdKey;

    MaceIdTagger(JavaPlugin plugin) {
        this.plugin = plugin;
        this.maceIdKey = new NamespacedKey(plugin, "mace-id");
    }

    Optional<UUID> readId(ItemStack stack) {
        if (!MaceItems.isMace(stack)) {
            return Optional.empty();
        }

        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return Optional.empty();
        }

        String rawMaceId = meta.getPersistentDataContainer().get(maceIdKey, PersistentDataType.STRING);
        if (rawMaceId == null || rawMaceId.isBlank()) {
            return Optional.empty();
        }

        try {
            return Optional.of(UUID.fromString(rawMaceId));
        } catch (IllegalArgumentException exception) {
            plugin.getLogger().warning("Invalid mace ID on item stack: " + rawMaceId);
            return Optional.empty();
        }
    }

    void tag(ItemStack mace, UUID maceId) {
        if (!MaceItems.isMace(mace)) {
            return;
        }

        ItemMeta meta = mace.getItemMeta();
        if (meta == null) {
            return;
        }

        String existingMaceId = meta.getPersistentDataContainer().get(maceIdKey, PersistentDataType.STRING);
        if (existingMaceId != null && existingMaceId.equals(maceId.toString())) {
            return;
        }

        meta.getPersistentDataContainer().set(maceIdKey, PersistentDataType.STRING, maceId.toString());
        mace.setItemMeta(meta);
    }
}
