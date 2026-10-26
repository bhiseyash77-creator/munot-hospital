package com.munot.hospital.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

/** Splits a date range into day / week (Mon-Sun) / calendar-month buckets for the reports. Pure and unit-tested. */
public final class ReportBuckets {
    private ReportBuckets() {}

    public enum Period { DAY, WEEK, MONTH }

    public record Bucket(LocalDate start, LocalDate end) {
        public boolean contains(LocalDate d) { return !d.isBefore(start) && !d.isAfter(end); }
    }

    public static final int MAX_BUCKETS = 400;

    /** Default range: last 30 days / last 12 weeks / last 12 months, always ending today. */
    public static LocalDate[] defaultRange(Period p, LocalDate today) {
        return switch (p) {
            case DAY -> new LocalDate[]{today.minusDays(29), today};
            case WEEK -> new LocalDate[]{today.minusWeeks(11), today};
            case MONTH -> new LocalDate[]{today.minusMonths(11).withDayOfMonth(1), today};
        };
    }

    /** Full buckets covering [from, to] (the first/last bucket may extend beyond the range). */
    public static List<Bucket> buckets(Period p, LocalDate from, LocalDate to) {
        if (from == null || to == null || to.isBefore(from)) throw new IllegalArgumentException("'to' must not be before 'from'");
        List<Bucket> out = new ArrayList<>();
        LocalDate cur = switch (p) {
            case DAY -> from;
            case WEEK -> from.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case MONTH -> from.withDayOfMonth(1);
        };
        while (!cur.isAfter(to)) {
            LocalDate end = switch (p) {
                case DAY -> cur;
                case WEEK -> cur.plusDays(6);
                case MONTH -> cur.with(TemporalAdjusters.lastDayOfMonth());
            };
            out.add(new Bucket(cur, end));
            if (out.size() > MAX_BUCKETS) throw new IllegalArgumentException("Range too long for this view. Choose a shorter range or a larger period.");
            cur = end.plusDays(1);
        }
        return out;
    }

    /** Index of the bucket containing the date, or -1. */
    public static int indexOf(List<Bucket> buckets, LocalDate d) {
        for (int i = 0; i < buckets.size(); i++) if (buckets.get(i).contains(d)) return i;
        return -1;
    }
}
