package com.munot.hospital.service;

import com.munot.hospital.dto.*;
import com.munot.hospital.dto.AdminDtos.NotificationView;
import com.munot.hospital.dto.AppointmentDtos.*;
import com.munot.hospital.dto.AuthDtos.UserView;
import com.munot.hospital.dto.BillingDtos.*;
import com.munot.hospital.dto.ClinicalDtos.*;
import com.munot.hospital.dto.PatientDtos.PatientView;
import com.munot.hospital.entity.*;
import com.munot.hospital.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Maps entities to API DTOs. Must be called inside a transaction (lazy associations). */
@Component
@RequiredArgsConstructor
public class ViewMapper {
    private final QueueEntryRepository queues;
    private final ConsultationRepository consultations;
    private final PrescriptionRepository prescriptions;
    private final FollowUpRepository followUps;
    private final DoctorRepository doctors;
    private final PatientRepository patients;
    private final SettingsService settings;

    public UserView user(User u) {
        Long patientId = null, doctorId = null;
        if (u.getRole() == Role.PATIENT) patientId = patients.findByUserId(u.getId()).map(Patient::getId).orElse(null);
        if (u.getRole() == Role.DOCTOR) doctorId = doctors.findByUserId(u.getId()).map(Doctor::getId).orElse(null);
        return new UserView(u.getId(), u.getFullName(), u.getEmail(), u.getPhone(), u.getRole(), patientId, doctorId, u.isEnabled());
    }

    public PatientView patient(Patient p) {
        return new PatientView(p.getId(), p.getPatientCode(), p.getFullName(), p.getPhone(), p.getEmail(), p.getDateOfBirth(), p.getGender(),
                p.getAddress(), p.getBloodGroup(), p.getEmergencyContactName(), p.getEmergencyContactPhone(), p.getUser() != null, p.getCreatedAt());
    }

    public DoctorView doctor(Doctor d) {
        Department dep = d.getDepartment();
        return new DoctorView(d.getId(), d.getUser().getId(), d.getUser().getFullName(), d.getUser().getEmail(), d.getUser().getPhone(),
                dep == null ? null : dep.getId(), dep == null ? null : dep.getName(), d.getSpecialization(), d.getQualification(),
                d.getRegistrationNumber(), d.getConsultationFee(), d.getBio(), d.getSlotMinutes(), d.getMaxPatientsPerSlot(), d.isActive(),
                d.getPhotoStoredName() != null, d.getUpdatedAt() == null ? 0L : d.getUpdatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());
    }

    public AppointmentView appointment(Appointment a) {
        Integer token = queues.findByAppointmentId(a.getId()).map(QueueEntry::getTokenNumber).orElse(null);
        Long consultationId = consultations.findByAppointmentId(a.getId()).map(Consultation::getId).orElse(null);
        int cutoff = settings.getInt("cancellation_cutoff_hours", 2);
        boolean open = a.getStatus() == AppointmentStatus.BOOKED || a.getStatus() == AppointmentStatus.CONFIRMED;
        boolean canModify = open && LocalDateTime.of(a.getAppointmentDate(), a.getStartTime()).isAfter(LocalDateTime.now().plusHours(cutoff));
        Patient p = a.getPatient();
        return new AppointmentView(a.getId(), a.getAppointmentCode(), p.getId(), p.getPatientCode(), p.getFullName(), p.getPhone(),
                a.getDoctor().getId(), a.getDoctor().getUser().getFullName(), a.getAppointmentDate(), a.getStartTime(), a.getEndTime(),
                a.getStatus(), a.getType(), a.getReason(), token, a.getDoctor().getConsultationFee(), consultationId, canModify);
    }

    public QueueView queue(QueueEntry q) {
        Appointment a = q.getAppointment();
        return new QueueView(q.getId(), a.getId(), a.getAppointmentCode(), a.getPatient().getId(), a.getPatient().getFullName(),
                a.getPatient().getPatientCode(), q.getDoctor().getId(), q.getDoctor().getUser().getFullName(), q.getTokenNumber(),
                q.getStatus(), q.getCheckedInAt(), q.getCalledAt(), a.getStartTime());
    }

    public FollowUpView followUp(FollowUp f) {
        return new FollowUpView(f.getId(), f.getPatient().getId(), f.getPatient().getFullName(), f.getDoctor().getUser().getFullName(),
                f.getFollowUpDate(), f.getFollowUpTime(), f.getStatus(), f.getAppointment() == null ? null : f.getAppointment().getId(),
                f.getAppointment() == null ? null : f.getAppointment().getAppointmentCode(), f.getNotes());
    }

    private List<PrescriptionItemDto> items(Prescription rx) {
        List<PrescriptionItemDto> out = new ArrayList<>();
        if (rx != null) rx.getItems().forEach(i -> out.add(new PrescriptionItemDto(i.getMedicineName(), i.getComposition(), i.getDosage(), i.getFrequency(), i.getDuration(), i.getInstructions(), i.getFrequencyLocal(), i.getDurationLocal(), i.getInstructionsLocal())));
        return out;
    }

    public ConsultationView consultation(Consultation c) {
        Prescription rx = prescriptions.findByConsultationId(c.getId()).orElse(null);
        List<DiagnosisDto> dx = c.getDiagnoses().stream().map(d -> new DiagnosisDto(d.getName(), d.getIcdCode(), d.getNotes())).toList();
        FollowUpView fu = followUps.findByConsultationId(c.getId()).map(this::followUp).orElse(null);
        return new ConsultationView(c.getId(), c.getAppointment().getId(), c.getAppointment().getAppointmentCode(), c.getPatient().getId(),
                c.getPatient().getFullName(), c.getDoctor().getId(), c.getDoctor().getUser().getFullName(), c.getSymptoms(),
                c.getExaminationNotes(), c.getTreatmentPlan(), c.getTestsAdvised(), c.getAdvice(), c.getStatus(), c.getStartedAt(),
                c.getCompletedAt(), dx, items(rx), rx == null ? null : rx.getNotes(), rx == null ? null : rx.getId(), fu,
                c.getLanguage(), c.getAdviceLocal(), c.getTestsAdvisedLocal());
    }

    public PrescriptionView prescription(Prescription rx) {
        List<String> dx = rx.getConsultation().getDiagnoses().stream().map(Diagnosis::getName).toList();
        return new PrescriptionView(rx.getId(), rx.getPrescriptionCode(), rx.getConsultation().getId(), rx.getPatient().getId(),
                rx.getPatient().getFullName(), rx.getDoctor().getUser().getFullName(), rx.getCreatedAt(), rx.getNotes(), items(rx), dx,
                rx.getPatient().getPatientCode(), rx.getPatient().getPhone(), rx.getPatient().getDateOfBirth(), rx.getPatient().getGender(),
                rx.getDoctor().getQualification(), rx.getDoctor().getRegistrationNumber(), rx.getConsultation().getAdvice(), rx.getConsultation().getTestsAdvised(),
                rx.getConsultation().getLanguage(), rx.getConsultation().getAdviceLocal(), rx.getConsultation().getTestsAdvisedLocal());
    }

    public ReportView report(MedicalReport r) {
        PatientDocument d = r.getDocument();
        return new ReportView(r.getId(), r.getPatient().getId(), r.getAppointment() == null ? null : r.getAppointment().getId(),
                r.getReportType(), r.getTitle(), r.getNotes(), r.getReportDate(), d == null ? null : d.getId(),
                d == null ? null : d.getFileName(), d == null ? null : d.getContentType(), d == null ? null : d.getSizeBytes());
    }

    public BillView bill(Bill b) {
        List<BillItemView> items = b.getItems().stream().map(i -> new BillItemView(i.getId(), i.getCategory(), i.getDescription(),
                i.getQuantity(), i.getUnitPrice(), i.getAmount())).toList();
        List<PaymentView> pays = b.getPayments().stream().map(p -> new PaymentView(p.getId(), p.getAmount(), p.getMethod(), p.getReference(), p.getPaidAt())).toList();
        return new BillView(b.getId(), b.getBillNumber(), b.getPatient().getId(), b.getPatient().getFullName(), b.getPatient().getPatientCode(),
                b.getAppointment() == null ? null : b.getAppointment().getId(), items, b.getSubtotal(), b.getDiscount(), b.getTaxPercent(),
                b.getTax(), b.getTotal(), b.getPaidAmount(), b.getPendingAmount(), b.getStatus(), b.getNotes(), b.getCreatedAt(), pays);
    }

    public NotificationView notification(Notification n) {
        return new NotificationView(n.getId(), n.getChannel(), n.getType(), n.getTitle(), n.getMessage(), n.getStatus(),
                n.getScheduledAt(), n.getSentAt(), n.isReadFlag());
    }
}
