package com.munot.hospital.util;

import com.munot.hospital.entity.AppointmentStatus;
import com.munot.hospital.entity.Role;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.time.LocalTime;
import static org.junit.jupiter.api.Assertions.*;

class AppointmentRulesTest {
    @Test
    void onlinePatientRequestsNeedConfirmationButStaffBookingsDoNot() {
        assertTrue(AppointmentRules.needsStaffConfirmation(Role.PATIENT));
        assertFalse(AppointmentRules.needsStaffConfirmation(Role.RECEPTIONIST));
        assertFalse(AppointmentRules.needsStaffConfirmation(Role.DOCTOR));
        assertFalse(AppointmentRules.needsStaffConfirmation(Role.ADMIN));
    }

    @Test
    void onlyBookedCanBeConfirmed() {
        assertTrue(AppointmentRules.canConfirm(AppointmentStatus.BOOKED));
        for (AppointmentStatus s : AppointmentStatus.values()) if (s != AppointmentStatus.BOOKED) assertFalse(AppointmentRules.canConfirm(s), s.name());
    }

    @Test
    void checkInOnlyFromBookedOrConfirmed() {
        assertTrue(AppointmentRules.canCheckIn(AppointmentStatus.BOOKED));
        assertTrue(AppointmentRules.canCheckIn(AppointmentStatus.CONFIRMED));
        assertFalse(AppointmentRules.canCheckIn(AppointmentStatus.COMPLETED));
        assertFalse(AppointmentRules.canCheckIn(AppointmentStatus.CANCELLED));
        assertFalse(AppointmentRules.canCheckIn(AppointmentStatus.WAITING));
    }

    @Test
    void patientRescheduleNeedsReconfirmationStaffRescheduleKeepsStatus() {
        assertEquals(AppointmentStatus.BOOKED, AppointmentRules.statusAfterReschedule(AppointmentStatus.CONFIRMED, Role.PATIENT));
        assertEquals(AppointmentStatus.CONFIRMED, AppointmentRules.statusAfterReschedule(AppointmentStatus.CONFIRMED, Role.RECEPTIONIST));
    }

    @Test
    void advanceBookingLimit() {
        LocalDate today = LocalDate.of(2026, 10, 10);
        assertTrue(AppointmentRules.withinAdvanceLimit(today, today.plusDays(60), 60));
        assertFalse(AppointmentRules.withinAdvanceLimit(today, today.plusDays(61), 60));
    }
}
