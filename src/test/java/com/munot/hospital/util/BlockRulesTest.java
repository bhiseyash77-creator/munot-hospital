package com.munot.hospital.util;

import org.junit.jupiter.api.Test;
import java.time.LocalTime;
import static org.junit.jupiter.api.Assertions.*;

class BlockRulesTest {
    private static LocalTime t(int h, int m) { return LocalTime.of(h, m); }

    @Test
    void wholeDayBlockCoversEverySlot() {
        assertTrue(BlockRules.overlaps(null, null, t(10, 0), t(10, 15)));
    }

    @Test
    void timeRangeBlocksOnlyOverlappingSlots() {
        assertTrue(BlockRules.overlaps(t(11, 0), t(12, 0), t(11, 0), t(11, 15)));
        assertTrue(BlockRules.overlaps(t(11, 0), t(12, 0), t(11, 45), t(12, 0)));
        assertTrue(BlockRules.overlaps(t(11, 0), t(12, 0), t(10, 50), t(11, 5)));   // straddles the start
        assertFalse(BlockRules.overlaps(t(11, 0), t(12, 0), t(10, 45), t(11, 0)));  // ends exactly at block start
        assertFalse(BlockRules.overlaps(t(11, 0), t(12, 0), t(12, 0), t(12, 15)));  // starts exactly at block end
    }

    @Test
    void rangeValidation() {
        assertTrue(BlockRules.validRange(null, null));
        assertTrue(BlockRules.validRange(t(9, 0), t(10, 0)));
        assertFalse(BlockRules.validRange(t(9, 0), null));
        assertFalse(BlockRules.validRange(null, t(9, 0)));
        assertFalse(BlockRules.validRange(t(10, 0), t(10, 0)));
        assertFalse(BlockRules.validRange(t(11, 0), t(10, 0)));
    }
}
