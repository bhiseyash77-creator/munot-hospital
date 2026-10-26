package com.munot.hospital.controller;

import com.munot.hospital.dto.ReportDtos.*;
import com.munot.hospital.service.ReportService;
import com.munot.hospital.util.ReportBuckets.Period;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/reports")
@PreAuthorize("hasAnyRole('ADMIN','DOCTOR')")
public class ReportController {
    private final ReportService reports;

    public ReportController(ReportService reports) { this.reports = reports; }

    /** Doctors see the same hospital-wide reports as the admin. */
    @GetMapping
    public ReportResponse report(@RequestParam(defaultValue = "DAY") Period period,
                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reports.build(period, from, to);
    }
}
