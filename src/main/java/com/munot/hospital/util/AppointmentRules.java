package com.munot.hospital.util;

import com.munot.hospital.entity.AppointmentStatus;
import com.munot.hospital.entity.Role;
import java.time.LocalDate;

/** Pure appointment workflow rules (kept free of Spring so they are unit-testable). */
public final class AppointmentRules {
    private AppointmentRules() {}

    /** Online requests from patients must be confirmed by the doctor or reception. Staff-created bookings are confirmed immediately. */
    public static boolean needsStaffConfirmation(Role creator) { return creator == Role.PATIENT; }

    public static boolean canConfirm(AppointmentStatus s) { return s == AppointmentStatus.BOOKED; }

    public static boolean canCheckIn(AppointmentStatus s) { return s == AppointmentStatus.BOOKED || s == AppointmentStatus.CONFIRMED; }

    public static boolean isOpen(AppointmentStatus s) { return s == AppointmentStatus.BOOKED || s == AppointmentStatus.CONFIRMED; }

    /** A patient who changes the time has to be re-confirmed; staff changes keep the current status. */
    public static AppointmentStatus statusAfterReschedule(AppointmentStatus current, Role actor) {
        return actor == Role.PATIENT ? AppointmentStatus.BOOKED : current;
    }

    public static boolean withinAdvanceLimit(LocalDate today, LocalDate date, int maxDays) {
        return !date.isAfter(today.plusDays(maxDays));
    }
}
