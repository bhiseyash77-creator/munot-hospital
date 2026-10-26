package com.munot.hospital.service;

import com.munot.hospital.common.ApiException;
import com.munot.hospital.dto.ReportDtos.*;
import com.munot.hospital.entity.*;
import com.munot.hospital.repository.*;
import com.munot.hospital.util.ReportBuckets;
import com.munot.hospital.util.ReportBuckets.Bucket;
import com.munot.hospital.util.ReportBuckets.Period;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/** Day-wise / week-wise / month-wise hospital reports (appointments, outcomes, new patients, billing, collections). */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {
    private final AppointmentRepository appointments;
    private final PatientRepository patients;
    private final BillRepository bills;
    private final PaymentRepository payments;
    private final AuditService audit;

    private static final class Acc {
        long appts, completed, cancelled, noShow, walkIns, online, followUps, newPatients;
        BigDecimal billed = BigDecimal.ZERO, collected = BigDecimal.ZERO;
    }

    private static ReportRow row(Bucket b, Acc a) {
        return new ReportRow(b.start(), b.end(), a.appts, a.completed, a.cancelled, a.noShow, a.walkIns, a.online, a.followUps, a.newPatients, a.billed, a.collected);
    }

    public ReportResponse build(Period period, LocalDate from, LocalDate to) {
        LocalDate today = LocalDate.now();
        LocalDate[] def = ReportBuckets.defaultRange(period, today);
        LocalDate f = from == null ? def[0] : from;
        LocalDate t = to == null ? def[1] : to;
        List<Bucket> buckets;
        try {
            buckets = ReportBuckets.buckets(period, f, t);
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest(e.getMessage());
        }
        LocalDate first = buckets.get(0).start();
        LocalDate last = buckets.get(buckets.size() - 1).end();
        List<Acc> acc = new ArrayList<>();
        for (int i = 0; i < buckets.size(); i++) acc.add(new Acc());

        for (Appointment a : appointments.findByAppointmentDateBetween(first, last)) {
            int i = ReportBuckets.indexOf(buckets, a.getAppointmentDate());
            if (i < 0) continue;
            Acc x = acc.get(i);
            x.appts++;
            switch (a.getStatus()) {
                case COMPLETED -> x.completed++;
                case CANCELLED -> x.cancelled++;
                case NO_SHOW -> x.noShow++;
                default -> { }
            }
            switch (a.getType()) {
                case WALK_IN -> x.walkIns++;
                case FOLLOW_UP -> x.followUps++;
                default -> x.online++;
            }
        }
        for (Patient p : patients.findByCreatedAtBetween(first.atStartOfDay(), last.plusDays(1).atStartOfDay())) {
            int i = ReportBuckets.indexOf(buckets, p.getCreatedAt().toLocalDate());
            if (i >= 0) acc.get(i).newPatients++;
        }
        for (Bill b : bills.findByCreatedAtBetween(first.atStartOfDay(), last.plusDays(1).atStartOfDay())) {
            int i = ReportBuckets.indexOf(buckets, b.getCreatedAt().toLocalDate());
            if (i >= 0) acc.get(i).billed = acc.get(i).billed.add(b.getTotal());
        }
        for (Payment p : payments.findByPaidAtBetween(first.atStartOfDay(), last.plusDays(1).atStartOfDay())) {
            int i = ReportBuckets.indexOf(buckets, p.getPaidAt().toLocalDate());
            if (i >= 0) acc.get(i).collected = acc.get(i).collected.add(p.getAmount());
        }

        List<ReportRow> rows = new ArrayList<>();
        Acc total = new Acc();
        for (int i = 0; i < buckets.size(); i++) {
            Acc x = acc.get(i);
            rows.add(row(buckets.get(i), x));
            total.appts += x.appts; total.completed += x.completed; total.cancelled += x.cancelled; total.noShow += x.noShow;
            total.walkIns += x.walkIns; total.online += x.online; total.followUps += x.followUps; total.newPatients += x.newPatients;
            total.billed = total.billed.add(x.billed); total.collected = total.collected.add(x.collected);
        }
        audit.log("REPORT_VIEWED", "Report", period.name(), first + " to " + last);
        BigDecimal outstanding = Optional.ofNullable(bills.totalPending()).orElse(BigDecimal.ZERO);
        return new ReportResponse(period, first, last, row(new Bucket(first, last), total), rows, outstanding);
    }
}
