package com.munot.hospital.dto;

import com.munot.hospital.entity.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public final class AdminDtos {
    private AdminDtos() {}

    private static final String PW = "^(?=.*[A-Za-z])(?=.*\\d).+$";

    public record DoctorRequest(@NotBlank @Size(max = 120) String fullName, @NotBlank @Email String email,
                                @Pattern(regexp = "^[+0-9 \\-]{10,15}$") String phone,
                                @Size(min = 8, max = 72) String password, Long departmentId, @Size(max = 150) String specialization,
                                @Size(max = 255) String qualification, @Size(max = 80) String registrationNumber,
                                @NotNull @DecimalMin("0.00") BigDecimal consultationFee, String bio,
                                @Min(5) @Max(120) int slotMinutes, @Min(1) @Max(20) int maxPatientsPerSlot, boolean active) {}

    public record StaffRequest(@NotBlank @Size(max = 120) String fullName, @NotBlank @Email String email,
                               @Pattern(regexp = "^[+0-9 \\-]{10,15}$") String phone, @Size(min = 8, max = 72) String password,
                               boolean enabled) {}

    public record DepartmentRequest(@NotBlank @Size(max = 100) String name, @Size(max = 500) String description) {}

    public record DepartmentView(Long id, String name, String description) {}

    public record ServiceRequest(@NotBlank @Size(max = 150) String name, @Size(max = 600) String description, Long departmentId,
                                 @NotNull @DecimalMin("0.00") BigDecimal price, boolean active) {}

    public record ServiceView(Long id, String name, String description, Long departmentId, String departmentName, BigDecimal price, boolean active) {}

    public record NotificationView(Long id, Channel channel, NotificationType type, String title, String message,
                                   NotificationStatus status, LocalDateTime scheduledAt, LocalDateTime sentAt, boolean read) {}

    public record DailyPoint(String date, BigDecimal value) {}

    public record AdminSummary(long patients, long doctors, long appointmentsToday, long completedToday, BigDecimal revenueToday,
                               BigDecimal pendingAmount, Map<String, Long> statusBreakdown, List<DailyPoint> appointmentsPerDay,
                               List<DailyPoint> revenuePerDay) {}

    public record AuditView(Long id, String username, String action, String entityType, String entityId, String details,
                            String ipAddress, LocalDateTime createdAt) {}
}
