//package com.munot.hospital.util;
//
//import com.munot.hospital.entity.FollowUpOption;
//import org.junit.jupiter.api.Test;
//import java.time.LocalDate;
//import static org.junit.jupiter.api.Assertions.*;
//
//class FollowUpCalcTest {
//    private final LocalDate visit = LocalDate.of(2026, 10, 8);
//
//    @Test
//    void presetOffsets() {
//        assertEquals(LocalDate.of(2026, 10, 11), FollowUpCalc.targetDate(visit, FollowUpOption.DAYS_3, null));
//        assertEquals(LocalDate.of(2026, 10, 15), FollowUpCalc.targetDate(visit, FollowUpOption.DAYS_7, null));
//        assertEquals(LocalDate.of(2026, 11, 7), FollowUpCalc.targetDate(visit, FollowUpOption.DAYS_30, null));
//        assertNull(FollowUpCalc.targetDate(visit, FollowUpOption.NONE, null));
//    }
//
//    @Test
//    void customDateMustBeInFuture() {
//        assertEquals(LocalDate.of(2026, 10, 15), FollowUpCalc.targetDate(visit, FollowUpOption.CUSTOM, LocalDate.of(2026, 10, 15)));
//        assertThrows(IllegalArgumentException.class, () -> FollowUpCalc.targetDate(visit, FollowUpOption.CUSTOM, visit));
//    }
//}
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

    private static LocalDate days(LocalDate d, Integer n) { return FollowUpCalc.targetDate(d, FollowUpOption.DAYS, null, n); }

    @Test
    void anyNumberOfDaysIsAddedToTheVisitDate() {
        assertEquals(LocalDate.of(2026, 11, 6), days(LocalDate.of(2026, 10, 30), 7)); // month change
        assertEquals(LocalDate.of(2026, 10, 11), days(visit, 3));
        assertEquals(LocalDate.of(2026, 10, 13), days(visit, 5));
        assertEquals(LocalDate.of(2026, 10, 18), days(visit, 10));
        assertEquals(LocalDate.of(2026, 10, 23), days(visit, 15));
        assertEquals(LocalDate.of(2026, 10, 28), days(visit, 20));
        assertEquals(LocalDate.of(2026, 11, 7), days(visit, 30));
    }

    @Test
    void yearEndAndLeapYear() {
        assertEquals(LocalDate.of(2027, 1, 4), days(LocalDate.of(2026, 12, 28), 7));
        assertEquals(LocalDate.of(2028, 3, 1), days(LocalDate.of(2028, 2, 27), 3));  // 2028 is a leap year (29 Feb exists)
        assertEquals(LocalDate.of(2027, 3, 2), days(LocalDate.of(2027, 2, 27), 3));  // 2027 is not
    }

    @Test
    void invalidDaysAreRejectedAndNoneStaysNone() {
        for (Integer bad : new Integer[]{null, 0, -1, -30, 366}) assertThrows(IllegalArgumentException.class, () -> days(visit, bad));
        assertNull(FollowUpCalc.targetDate(visit, FollowUpOption.NONE, null, 7));
        assertEquals(LocalDate.of(2026, 10, 15), FollowUpCalc.targetDate(visit, FollowUpOption.DAYS_7, null, 99)); // presets ignore days
    }
}
