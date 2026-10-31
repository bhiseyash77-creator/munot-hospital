//
//package com.munot.hospital.dto;
//
//import com.munot.hospital.entity.*;
//import jakarta.validation.Valid;
//import jakarta.validation.constraints.*;
//import java.math.BigDecimal;
//import java.time.*;
//import java.util.*;
//
//public final class ClinicalDtos {
//    private ClinicalDtos() {}
//
//    public record DiagnosisDto(@NotBlank @Size(max = 200) String name, @Size(max = 20) String icdCode, @Size(max = 500) String notes) {}
//
//    public record PrescriptionItemDto(@NotBlank @Size(max = 200) String medicineName, @Size(max = 300) String composition, @Size(max = 100) String dosage,
//                                      @Size(max = 100) String frequency, @Size(max = 100) String duration,
//                                      @Size(max = 300) String instructions, @Size(max = 300) String frequencyLocal,
//                                      @Size(max = 300) String durationLocal, @Size(max = 300) String instructionsLocal) {}
//
//    public record ConsultationSaveRequest(String symptoms, String examinationNotes, String treatmentPlan, String testsAdvised,
//                                          String advice, @Valid List<DiagnosisDto> diagnoses, @Valid List<PrescriptionItemDto> items,
//                                          String prescriptionNotes, @Pattern(regexp = "^(EN|MR|HI)$") String language,
//                                          @Size(max = 5000) String adviceLocal, @Size(max = 5000) String testsAdvisedLocal) {}
//
//    public record CompleteRequest(@Valid ConsultationSaveRequest consultation, @NotNull FollowUpOption followUp,
//                                  LocalDate customDate, LocalTime customTime) {}
//
//    public record ConsultationView(Long id, Long appointmentId, String appointmentCode, Long patientId, String patientName,
//                                   Long doctorId, String doctorName, String symptoms, String examinationNotes, String treatmentPlan,
//                                   String testsAdvised, String advice, ConsultationStatus status, LocalDateTime startedAt,
//                                   LocalDateTime completedAt, List<DiagnosisDto> diagnoses, List<PrescriptionItemDto> items,
//                                   String prescriptionNotes, Long prescriptionId, FollowUpView followUp,
//                                   String language, String adviceLocal, String testsAdvisedLocal) {}
//
//    public record PrescriptionView(Long id, String code, Long consultationId, Long patientId, String patientName, String doctorName,
//                                   LocalDateTime date, String notes, List<PrescriptionItemDto> items, List<String> diagnoses,
//                                   String patientCode, String patientPhone, LocalDate patientDob, String patientGender,
//                                   String doctorQualification, String doctorRegistrationNumber, String advice, String testsAdvised,
//                                   String language, String adviceLocal, String testsAdvisedLocal,
//                                   String symptoms, String examinationNotes) {}
//
//    public record FollowUpView(Long id, Long patientId, String patientName, String doctorName, LocalDate date, LocalTime time,
//                               FollowUpStatus status, Long appointmentId, String appointmentCode, String notes) {}
//
//    public record CompleteResponse(ConsultationView consultation, FollowUpView followUp, String followUpMessage) {}
//
//    public record ReportView(Long id, Long patientId, Long appointmentId, ReportType reportType, String title, String notes,
//                             LocalDate reportDate, Long documentId, String fileName, String contentType, Long sizeBytes) {}
//
//    public record HistoryView(PatientDtos.PatientView patient, List<AppointmentDtos.AppointmentView> appointments,
//                              List<ConsultationView> consultations, List<PrescriptionView> prescriptions, List<FollowUpView> followUps,
//                              List<ReportView> reports, List<BillingDtos.BillView> bills) {}
//
//    public record MedicineView(Long id, String name, String composition, int useCount) {}
//}

package com.munot.hospital.dto;

import com.munot.hospital.entity.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public final class ClinicalDtos {
    private ClinicalDtos() {}

    public record DiagnosisDto(@NotBlank @Size(max = 200) String name, @Size(max = 20) String icdCode, @Size(max = 500) String notes) {}

    public record PrescriptionItemDto(@NotBlank @Size(max = 200) String medicineName, @Size(max = 300) String composition, @Size(max = 100) String dosage,
                                      @Size(max = 100) String frequency, @Size(max = 100) String duration,
                                      @Size(max = 300) String instructions, @Size(max = 300) String frequencyLocal,
                                      @Size(max = 300) String durationLocal, @Size(max = 300) String instructionsLocal) {}

    public record ConsultationSaveRequest(String symptoms, String examinationNotes, String treatmentPlan, String testsAdvised,
                                          String advice, @Valid List<DiagnosisDto> diagnoses, @Valid List<PrescriptionItemDto> items,
                                          String prescriptionNotes, @Pattern(regexp = "^(EN|MR|HI)$") String language,
                                          @Size(max = 5000) String adviceLocal, @Size(max = 5000) String testsAdvisedLocal,
                                          @Min(1) @Max(365) Integer followUpDays) {}

    public record CompleteRequest(@Valid ConsultationSaveRequest consultation, @NotNull FollowUpOption followUp,
                                  LocalDate customDate, LocalTime customTime) {}

    public record ConsultationView(Long id, Long appointmentId, String appointmentCode, Long patientId, String patientName,
                                   Long doctorId, String doctorName, String symptoms, String examinationNotes, String treatmentPlan,
                                   String testsAdvised, String advice, ConsultationStatus status, LocalDateTime startedAt,
                                   LocalDateTime completedAt, List<DiagnosisDto> diagnoses, List<PrescriptionItemDto> items,
                                   String prescriptionNotes, Long prescriptionId, FollowUpView followUp,
                                   String language, String adviceLocal, String testsAdvisedLocal, Integer followUpDays) {}

    public record PrescriptionView(Long id, String code, Long consultationId, Long patientId, String patientName, String doctorName,
                                   LocalDateTime date, String notes, List<PrescriptionItemDto> items, List<String> diagnoses,
                                   String patientCode, String patientPhone, LocalDate patientDob, String patientGender,
                                   String doctorQualification, String doctorRegistrationNumber, String advice, String testsAdvised,
                                   String language, String adviceLocal, String testsAdvisedLocal,
                                   String symptoms, String examinationNotes, Integer followUpDays) {}

    public record FollowUpView(Long id, Long patientId, String patientName, String doctorName, LocalDate date, LocalTime time,
                               FollowUpStatus status, Long appointmentId, String appointmentCode, String notes) {}

    public record CompleteResponse(ConsultationView consultation, FollowUpView followUp, String followUpMessage) {}

    public record ReportView(Long id, Long patientId, Long appointmentId, ReportType reportType, String title, String notes,
                             LocalDate reportDate, Long documentId, String fileName, String contentType, Long sizeBytes) {}

    public record HistoryView(PatientDtos.PatientView patient, List<AppointmentDtos.AppointmentView> appointments,
                              List<ConsultationView> consultations, List<PrescriptionView> prescriptions, List<FollowUpView> followUps,
                              List<ReportView> reports, List<BillingDtos.BillView> bills) {}

    public record MedicineView(Long id, String name, String composition, int useCount) {}
}
