package org.bonkmc.limitedmaces.storage;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class VersionComparatorTest {
    @Test
    void detectsNewerMultiDigitVersionComponents() {
        assertTrue(VersionComparator.isNewer("1.2.10", "1.2.9"));
    }

    @Test
    void treatsMissingVersionComponentsAsZero() {
        assertFalse(VersionComparator.isNewer("1.2", "1.2.0"));
        assertTrue(VersionComparator.isOlderOrEqual("1.2", "1.2.0"));
    }

    @Test
    void rejectsOlderVersionsAsUpdates() {
        assertFalse(VersionComparator.isNewer("1.2.0", "1.2.1"));
    }
}
