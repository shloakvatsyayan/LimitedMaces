package org.bonkmc.limitedmaces.storage;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MaceRecordTest {
    @Test
    void recognizesCurrentHeldOwner() {
        UUID holderId = UUID.randomUUID();
        MaceRecord record = heldRecord(holderId);

        assertTrue(record.isHeldBy(holderId));
    }

    @Test
    void rejectsPreviousOwnerWhileStillMarkedHeld() {
        MaceRecord record = heldRecord(UUID.randomUUID());

        assertFalse(record.isHeldBy(UUID.randomUUID()));
    }

    @Test
    void rejectsMatchingOwnerWhenMaceIsDropped() {
        UUID holderId = UUID.randomUUID();
        MaceRecord record = heldRecord(holderId);
        record.status = "DROPPED";

        assertFalse(record.isHeldBy(holderId));
    }

    private MaceRecord heldRecord(UUID holderId) {
        MaceRecord record = new MaceRecord();
        record.lastHolder = holderId;
        record.status = "HELD";
        return record;
    }
}
