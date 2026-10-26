package com.munot.hospital.util;

import com.munot.hospital.util.ReportBuckets.Period;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class ReportBucketsTest {
    private static LocalDate d(int y, int m, int day) { return LocalDate.of(y, m, day); }

    @Test
    void dayBucketsAreInclusive() {
        var b = ReportBuckets.buckets(Period.DAY, d(2026, 10, 1), d(2026, 10, 10));
        assertEquals(10, b.size());
        assertEquals(d(2026, 10, 10), b.get(9).start());
        assertEquals(b.get(0).start(), b.get(0).end());
    }

    @Test
    void weeksRunMondayToSundayAndCoverTheWholeRange() {
        // 2026-10-07 is a Wednesday -> first week starts Monday 2026-10-05
        var b = ReportBuckets.buckets(Period.WEEK, d(2026, 10, 7), d(2026, 10, 20));
        assertEquals(3, b.size());
        assertEquals(d(2026, 10, 5), b.get(0).start());
        assertEquals(d(2026, 10, 11), b.get(0).end());
        assertEquals(d(2026, 10, 19), b.get(2).start());
        assertEquals(d(2026, 10, 25), b.get(2).end());
    }

    @Test
    void monthsAreCalendarMonthsIncludingLeapFebruary() {
        var b = ReportBuckets.buckets(Period.MONTH, d(2027, 12, 15), d(2028, 3, 2));
        assertEquals(4, b.size());
        assertEquals(d(2027, 12, 1), b.get(0).start());
        assertEquals(d(2027, 12, 31), b.get(0).end());
        assertEquals(d(2028, 2, 29), b.get(2).end());      // 2028 is a leap year
    }

    @Test
    void everyDateFallsInExactlyOneBucket() {
        var b = ReportBuckets.buckets(Period.WEEK, d(2026, 1, 1), d(2026, 12, 31));
        for (LocalDate x = d(2026, 1, 1); !x.isAfter(d(2026, 12, 31)); x = x.plusDays(1)) {
            int hits = 0;
            for (var bk : b) if (bk.contains(x)) hits++;
            assertEquals(1, hits, x.toString());
            assertTrue(ReportBuckets.indexOf(b, x) >= 0);
        }
        assertEquals(-1, ReportBuckets.indexOf(b, d(2027, 6, 1)));
    }

    @Test
    void defaultRangesEndTodayAndTooLongOrReversedRangesAreRejected() {
        LocalDate today = d(2026, 10, 15);
        assertEquals(today, ReportBuckets.defaultRange(Period.DAY, today)[1]);
        assertEquals(d(2025, 11, 1), ReportBuckets.defaultRange(Period.MONTH, today)[0]);
        assertThrows(IllegalArgumentException.class, () -> ReportBuckets.buckets(Period.DAY, d(2026, 10, 10), d(2026, 10, 1)));
        assertThrows(IllegalArgumentException.class, () -> ReportBuckets.buckets(Period.DAY, d(2024, 1, 1), d(2026, 1, 1)));
    }
}
