package com.munot.hospital.dto;

import com.munot.hospital.entity.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public final class AuthDtos {
    private AuthDtos() {}

    public record LoginRequest(@NotBlank String email, @NotBlank String password) {}

    public record RegisterRequest(
            @NotBlank @Size(max = 120) String fullName,
            @NotBlank @Email @Size(max = 150) String email,
            @NotBlank @Pattern(regexp = "^[+0-9 \\-]{10,15}$", message = "Enter a valid phone number") String phone,
            @NotBlank @Size(min = 8, max = 72) @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "Password must contain a letter and a digit") String password,
            LocalDate dateOfBirth,
            @Size(max = 20) String gender) {}

    public record ChangePasswordRequest(@NotBlank String currentPassword,
            @NotBlank @Size(min = 8, max = 72) @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "Password must contain a letter and a digit") String newPassword) {}

    public record UserView(Long id, String fullName, String email, String phone, Role role, Long patientId, Long doctorId, boolean enabled) {}

    public record AuthResponse(String token, UserView user) {}
}
