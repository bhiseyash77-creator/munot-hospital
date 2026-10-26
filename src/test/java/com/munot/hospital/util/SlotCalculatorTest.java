package com.munot.hospital.util;

import org.junit.jupiter.api.Test;
import java.time.LocalTime;
import static org.junit.jupiter.api.Assertions.*;

class SlotCalculatorTest {
    @Test
    void skipsSlotsOverlappingBreak() {
        var s = SlotCalculator.generate(LocalTime.of(10, 0), LocalTime.of(13, 0), LocalTime.of(11, 0), LocalTime.of(11, 30), 15);
        assertEquals(10, s.size());
        assertTrue(s.stream().noneMatch(v -> v.start().equals(LocalTime.of(11, 0)) || v.start().equals(LocalTime.of(11, 15))));
        assertEquals(LocalTime.of(13, 0), s.get(s.size() - 1).end());
    }

    @Test
    void neverOverrunsClosingTimeOrWrapsMidnight() {
        assertEquals(2, SlotCalculator.generate(LocalTime.of(10, 0), LocalTime.of(10, 50), null, null, 20).size());
        assertEquals(1, SlotCalculator.generate(LocalTime.of(23, 0), LocalTime.of(23, 59), null, null, 30).size());
        assertTrue(SlotCalculator.generate(LocalTime.of(10, 0), LocalTime.of(12, 0), null, null, 0).isEmpty());
    }
}
