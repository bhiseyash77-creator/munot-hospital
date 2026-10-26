package com.munot.hospital.util;

import java.time.LocalTime;

public final class BlockRules {
    private BlockRules() {}

    /** A block with no times blocks the whole day; otherwise it blocks any slot overlapping [blockStart, blockEnd). */
    public static boolean overlaps(LocalTime blockStart, LocalTime blockEnd, LocalTime start, LocalTime end) {
        if (blockStart == null || blockEnd == null) return true;
        return start.isBefore(blockEnd) && end.isAfter(blockStart);
    }

    /** Both times or neither; end after start. */
    public static boolean validRange(LocalTime start, LocalTime end) {
        if (start == null && end == null) return true;
        return start != null && end != null && end.isAfter(start);
    }
}
