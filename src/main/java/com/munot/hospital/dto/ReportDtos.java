package com.munot.hospital.dto;

import com.munot.hospital.util.ReportBuckets.Period;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class ReportDtos {
    private ReportDtos() {}

    public record ReportRow(LocalDate start, LocalDate end, long appointments, long completed, long cancelled, long noShow, long walkIns,
                            long online, long followUps, long newPatients, BigDecimal billed, BigDecimal collected) {}

    /** rows = one per day/week/month; totals = sum of all rows; outstandingNow = unpaid bill balance across the hospital today. */
    public record ReportResponse(Period period, LocalDate from, LocalDate to, ReportRow totals, List<ReportRow> rows, BigDecimal outstandingNow) {}
}
