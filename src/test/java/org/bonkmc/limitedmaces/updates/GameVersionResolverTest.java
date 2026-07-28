package org.bonkmc.limitedmaces.updates;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class GameVersionResolverTest {
    private final GameVersionResolver versionResolver = new GameVersionResolver();

    @Test
    void resolvesPaper26BuildVersion() {
        assertEquals("26.2", versionResolver.resolve("26.2.build.84-stable"));
    }

    @Test
    void resolvesLegacyBukkitVersion() {
        assertEquals("1.21.8", versionResolver.resolve("1.21.8-R0.1-SNAPSHOT"));
    }

    @Test
    void rejectsUnrecognizedVersion() {
        assertThrows(
                IllegalStateException.class,
                () -> versionResolver.resolve("unknown-version")
        );
    }
}
