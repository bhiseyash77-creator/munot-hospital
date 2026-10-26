package com.munot.hospital.dto;

import com.munot.hospital.entity.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public final class PatientDtos {
    private PatientDtos() {}

    public record PatientRequest(
            @NotBlank @Size(max = 120) String fullName,
            @NotBlank @Pattern(regexp = "^[+0-9 \\-]{10,15}$", message = "Enter a valid phone number") String phone,
            @Email @Size(max = 150) String email,
            @PastOrPresent LocalDate dateOfBirth,
            @Size(max = 20) String gender,
            @Size(max = 300) String address,
            @Size(max = 10) String bloodGroup,
            @Size(max = 120) String emergencyContactName,
            @Size(max = 20) String emergencyContactPhone) {}

    public record PatientView(Long id, String patientCode, String fullName, String phone, String email, LocalDate dateOfBirth,
                              String gender, String address, String bloodGroup, String emergencyContactName,
                              String emergencyContactPhone, boolean hasPortalAccount, LocalDateTime createdAt) {}
}
