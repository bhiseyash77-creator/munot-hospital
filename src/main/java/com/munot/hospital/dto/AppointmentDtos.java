package com.munot.hospital.dto;

import com.munot.hospital.entity.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public final class AppointmentDtos {
    private AppointmentDtos() {}

    public record BookRequest(Long patientId, @NotNull Long doctorId, @NotNull LocalDate date, @NotNull LocalTime startTime,
                              @Size(max = 500) String reason, AppointmentType type) {}

    public record RescheduleRequest(@NotNull LocalDate date, @NotNull LocalTime startTime) {}

    public record CancelRequest(@Size(max = 300) String reason) {}

    public record AppointmentView(Long id, String code, Long patientId, String patientCode, String patientName, String patientPhone,
                                  Long doctorId, String doctorName, LocalDate date, LocalTime startTime, LocalTime endTime,
                                  AppointmentStatus status, AppointmentType type, String reason, Integer tokenNumber,
                                  BigDecimal consultationFee, Long consultationId, boolean canModify) {}

    public record SlotView(LocalTime startTime, LocalTime endTime, int remaining) {}

    public record QueueView(Long id, Long appointmentId, String appointmentCode, Long patientId, String patientName, String patientCode,
                            Long doctorId, String doctorName, int token, QueueStatus status, LocalDateTime checkedInAt,
                            LocalDateTime calledAt, LocalTime scheduledTime) {}

    public record DoctorView(Long id, Long userId, String fullName, String email, String phone, Long departmentId, String departmentName,
                             String specialization, String qualification, String registrationNumber, BigDecimal consultationFee,
                             String bio, int slotMinutes, int maxPatientsPerSlot, boolean active, boolean hasPhoto, long photoVersion) {}

    public record AvailabilityDto(@NotNull DayOfWeek dayOfWeek, @NotNull LocalTime startTime, @NotNull LocalTime endTime,
                                  LocalTime breakStart, LocalTime breakEnd, boolean active) {}

    public record LeaveRequest(@NotNull LocalDate date, @Size(max = 200) String reason) {}

    public record LeaveView(Long id, LocalDate date, String reason) {}

    public record BlockRequest(@NotNull LocalDate date, LocalTime startTime, LocalTime endTime, @Size(max = 200) String reason) {}

    public record BlockView(Long id, LocalDate date, LocalTime startTime, LocalTime endTime, String reason, boolean wholeDay, int affectedAppointments) {}
}
