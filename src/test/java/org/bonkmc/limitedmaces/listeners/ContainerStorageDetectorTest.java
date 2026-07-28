package org.bonkmc.limitedmaces.listeners;

import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.entity.minecart.StorageMinecart;
import org.bukkit.inventory.InventoryHolder;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ContainerStorageDetectorTest {
    private final ContainerStorageDetector storageDetector = new ContainerStorageDetector();

    @Test
    void ignoresDirectPlayerInventories() {
        Player inventoryOwner = player(UUID.randomUUID());
        Player viewer = player(UUID.randomUUID());

        assertFalse(storageDetector.isStorageInventory(inventoryOwner, viewer, true, false));
    }

    @Test
    void ignoresAnotherPlayersEnderChest() {
        Player inventoryOwner = player(UUID.randomUUID());
        Player viewer = player(UUID.randomUUID());

        assertFalse(storageDetector.isStorageInventory(inventoryOwner, viewer, false, true));
    }

    @Test
    void blocksTheViewersOwnEnderChest() {
        UUID playerId = UUID.randomUUID();
        Player inventoryOwner = player(playerId);
        Player viewer = player(playerId);

        assertTrue(storageDetector.isStorageInventory(inventoryOwner, viewer, false, true));
    }

    @Test
    void blocksBlockBackedContainers() {
        Container container = interfaceProxy(Container.class);

        assertTrue(storageDetector.isStorageInventory(container, player(UUID.randomUUID()), false, false));
    }

    @Test
    void blocksEntityBackedContainers() {
        StorageMinecart minecart = interfaceProxy(StorageMinecart.class);

        assertTrue(storageDetector.isStorageInventory(minecart, player(UUID.randomUUID()), false, false));
    }

    @Test
    void ignoresVirtualPluginInventories() {
        InventoryHolder virtualHolder = interfaceProxy(InventoryHolder.class);

        assertFalse(storageDetector.isStorageInventory(virtualHolder, player(UUID.randomUUID()), false, false));
    }

    @Test
    void ignoresHolderlessVirtualInventories() {
        assertFalse(storageDetector.isStorageInventory(null, player(UUID.randomUUID()), false, false));
    }

    private static Player player(UUID playerId) {
        return (Player) Proxy.newProxyInstance(
                Player.class.getClassLoader(),
                new Class<?>[]{Player.class},
                (proxy, method, arguments) -> method.getName().equals("getUniqueId")
                        ? playerId
                        : defaultValue(method.getReturnType())
        );
    }

    private static <T> T interfaceProxy(Class<T> interfaceClass) {
        Object interfaceProxy = Proxy.newProxyInstance(
                interfaceClass.getClassLoader(),
                new Class<?>[]{interfaceClass},
                (proxy, method, arguments) -> defaultValue(method.getReturnType())
        );
        return interfaceClass.cast(interfaceProxy);
    }

    private static Object defaultValue(Class<?> returnType) {
        if (!returnType.isPrimitive()) {
            return null;
        }
        if (returnType == boolean.class) {
            return false;
        }
        if (returnType == char.class) {
            return '\0';
        }
        return 0;
    }
}
