package com.munot.hospital.util;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public final class SlotCalculator {
    private SlotCalculator() {}

    public record Slot(LocalTime start, LocalTime end) {}

    /** Generates consecutive slots in [start,end) skipping any slot overlapping the break. */
    public static List<Slot> generate(LocalTime start, LocalTime end, LocalTime breakStart, LocalTime breakEnd, int minutes) {
        List<Slot> out = new ArrayList<>();
        if (start == null || end == null || minutes <= 0) return out;
        LocalTime s = start;
        while (true) {
            LocalTime e = s.plusMinutes(minutes);
            if (!e.isAfter(s) || e.isAfter(end)) break;
            boolean inBreak = breakStart != null && breakEnd != null && s.isBefore(breakEnd) && e.isAfter(breakStart);
            if (!inBreak) out.add(new Slot(s, e));
            s = e;
        }
        return out;
    }
}
