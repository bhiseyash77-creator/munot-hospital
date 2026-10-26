package com.munot.hospital.util;

import com.munot.hospital.entity.FollowUpOption;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class FollowUpCalcTest {
    private final LocalDate visit = LocalDate.of(2026, 10, 8);

    @Test
    void presetOffsets() {
        assertEquals(LocalDate.of(2026, 10, 11), FollowUpCalc.targetDate(visit, FollowUpOption.DAYS_3, null));
        assertEquals(LocalDate.of(2026, 10, 15), FollowUpCalc.targetDate(visit, FollowUpOption.DAYS_7, null));
        assertEquals(LocalDate.of(2026, 11, 7), FollowUpCalc.targetDate(visit, FollowUpOption.DAYS_30, null));
        assertNull(FollowUpCalc.targetDate(visit, FollowUpOption.NONE, null));
    }

    @Test
    void customDateMustBeInFuture() {
        assertEquals(LocalDate.of(2026, 10, 15), FollowUpCalc.targetDate(visit, FollowUpOption.CUSTOM, LocalDate.of(2026, 10, 15)));
        assertThrows(IllegalArgumentException.class, () -> FollowUpCalc.targetDate(visit, FollowUpOption.CUSTOM, visit));
    }
}
