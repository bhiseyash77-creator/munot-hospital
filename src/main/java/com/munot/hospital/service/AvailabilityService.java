package com.munot.hospital.service;

import com.munot.hospital.dto.AppointmentDtos.SlotView;
import com.munot.hospital.entity.*;
import com.munot.hospital.repository.*;
import com.munot.hospital.util.BlockRules;
import com.munot.hospital.util.SlotCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AvailabilityService {
    public static final List<AppointmentStatus> INACTIVE = List.of(AppointmentStatus.CANCELLED, AppointmentStatus.NO_SHOW);

    private final DoctorAvailabilityRepository availability;
    private final DoctorLeaveRepository leaves;
    private final AppointmentRepository appointments;
    private final SlotBlockRepository blocks;

    /**
     * Slots for a doctor on a date, honouring working days/hours, breaks, leave dates, duration and capacity.
     * @param allowOngoing when true (walk-ins) a slot that has started but not ended is still offered today.
     */
    public List<SlotView> slots(Doctor d, LocalDate date, boolean allowOngoing, Long excludeAppointmentId) {
        LocalDate today = LocalDate.now();
        if (!d.isActive() || date.isBefore(today) || leaves.existsByDoctorIdAndLeaveDate(d.getId(), date)) return List.of();
        Map<LocalTime, Long> booked = appointments.findByDoctorIdAndAppointmentDateAndStatusNotIn(d.getId(), date, INACTIVE).stream()
                .filter(a -> excludeAppointmentId == null || !a.getId().equals(excludeAppointmentId))
                .collect(Collectors.groupingBy(Appointment::getStartTime, Collectors.counting()));
        List<SlotBlock> dayBlocks = blocks.findByDoctorIdAndBlockDate(d.getId(), date);
        LocalTime now = LocalTime.now();
        TreeMap<LocalTime, SlotView> out = new TreeMap<>();
        for (DoctorAvailability w : availability.findByDoctorIdAndDayOfWeekAndActiveTrue(d.getId(), date.getDayOfWeek())) {
            for (SlotCalculator.Slot s : SlotCalculator.generate(w.getStartTime(), w.getEndTime(), w.getBreakStart(), w.getBreakEnd(), d.getSlotMinutes())) {
                if (date.equals(today)) {
                    boolean ok = allowOngoing ? s.end().isAfter(now) : s.start().isAfter(now);
                    if (!ok) continue;
                }
                if (dayBlocks.stream().anyMatch(b -> BlockRules.overlaps(b.getStartTime(), b.getEndTime(), s.start(), s.end()))) continue;
                int remaining = Math.max(d.getMaxPatientsPerSlot() - booked.getOrDefault(s.start(), 0L).intValue(), 0);
                out.putIfAbsent(s.start(), new SlotView(s.start(), s.end(), remaining));
            }
        }
        return new ArrayList<>(out.values());
    }

    public Optional<SlotView> findOpenSlot(Doctor d, LocalDate date, LocalTime start, boolean allowOngoing, Long excludeAppointmentId) {
        return slots(d, date, allowOngoing, excludeAppointmentId).stream()
                .filter(s -> s.startTime().equals(start) && s.remaining() > 0).findFirst();
    }

    /** First open slot on/after {@code preferred} on the given date. */
    public Optional<SlotView> nearestOnDate(Doctor d, LocalDate date, LocalTime preferred) {
        List<SlotView> open = slots(d, date, false, null).stream().filter(s -> s.remaining() > 0).toList();
        return open.stream().filter(s -> !s.startTime().isBefore(preferred)).findFirst().or(() -> open.stream().findFirst());
    }
}
