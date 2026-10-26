package com.munot.hospital.dto;

import com.munot.hospital.entity.MedicalDocStatus;
import com.munot.hospital.entity.MedicalDocType;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

public final class MedicalDocDtos {
    private MedicalDocDtos() {}

    public record CreateRequest(@NotNull Long patientId, @NotNull MedicalDocType docType, @NotNull LocalDate documentDate, @NotNull Map<String, String> content) {}

    public record UpdateRequest(@NotNull LocalDate documentDate, @NotNull Map<String, String> content) {}

    public record MedicalDocView(Long id, String code, MedicalDocType docType, MedicalDocStatus status, LocalDate documentDate, Long patientId,
                                 String patientName, String patientCode, Long doctorId, String doctorName, String doctorQualification,
                                 String doctorRegistrationNumber, Map<String, String> content, LocalDateTime issuedAt, Long revisionOfId,
                                 int printCount, LocalDateTime createdAt, LocalDateTime updatedAt) {}

    public record PrintSettings(@Min(0) @Max(150) int top, @Min(0) @Max(150) int bottom, @Min(0) @Max(60) int left, @Min(0) @Max(60) int right) {}
}
