//package com.munot.hospital.service;
//
//import com.munot.hospital.common.ApiException;
//import com.munot.hospital.dto.AppointmentDtos.SlotView;
//import com.munot.hospital.dto.BillingDtos.BillView;
//import com.munot.hospital.dto.ClinicalDtos.*;
//import com.munot.hospital.entity.*;
//import com.munot.hospital.repository.*;
//import com.munot.hospital.util.Codes;
//import com.munot.hospital.util.FollowUpCalc;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//import java.time.*;
//import java.util.*;
//
//@Service
//@RequiredArgsConstructor
//@Transactional(readOnly = true)
//public class ConsultationService {
//    private final ConsultationRepository consultations;
//    private final PrescriptionRepository prescriptions;
//    private final FollowUpRepository followUps;
//    private final QueueEntryRepository queues;
//    private final AppointmentRepository appointments;
//    private final MedicalReportRepository reports;
//    private final BillRepository bills;
//    private final AppointmentService appointmentService;
//    private final AvailabilityService availability;
//    private final NotificationService notifications;
//    private final MedicineService medicines;
//    private final AccessControl access;
//    private final ViewMapper mapper;
//    private final AuditService audit;
//
//    // ---------- consultation lifecycle ----------
//
//    /** Doctor calls a waiting patient in: creates the consultation and moves queue + appointment to IN_CONSULTATION. */
//    @Transactional
//    public ConsultationView start(Long appointmentId, User actor) {
//        Appointment a = access.requireAppointment(actor, appointmentId);
//        Doctor doctor = access.doctorOf(actor);
//        Consultation existing = consultations.findByAppointmentId(a.getId()).orElse(null);
//        if (existing != null) return mapper.consultation(existing);
//        if (a.getStatus() != AppointmentStatus.WAITING) throw ApiException.conflict("Patient must be checked in and waiting (current: " + a.getStatus() + ").");
//        if (queues.existsByDoctorIdAndQueueDateAndStatus(doctor.getId(), a.getAppointmentDate(), QueueStatus.IN_CONSULTATION)) {
//            throw ApiException.conflict("Please complete the current consultation before calling the next patient.");
//        }
//        QueueEntry q = queues.findByAppointmentId(a.getId()).orElseThrow(() -> ApiException.conflict("Patient is not in the queue"));
//        Consultation c = new Consultation();
//        c.setAppointment(a);
//        c.setPatient(a.getPatient());
//        c.setDoctor(doctor);
//        c.setStatus(ConsultationStatus.IN_PROGRESS);
//        c.setStartedAt(LocalDateTime.now());
//        consultations.save(c);
//        q.setStatus(QueueStatus.IN_CONSULTATION);
//        q.setCalledAt(LocalDateTime.now());
//        a.setStatus(AppointmentStatus.IN_CONSULTATION);
//        audit.log("CONSULTATION_STARTED", "Consultation", c.getId(), a.getAppointmentCode());
//        return mapper.consultation(c);
//    }
//
//    private Consultation own(Long id, User actor) {
//        Consultation c = consultations.findById(id).orElseThrow(() -> ApiException.notFound("Consultation not found"));
//        if (actor.getRole() != Role.DOCTOR || !c.getDoctor().getUser().getId().equals(actor.getId())) throw ApiException.forbidden("Access denied");
//        return c;
//    }
//
//    public ConsultationView get(Long id, User actor) {
//        Consultation c = own(id, actor);
//        audit.log("CONSULTATION_VIEWED", "Consultation", id, null);
//        return mapper.consultation(c);
//    }
//
//    @Transactional
//    public ConsultationView save(Long id, ConsultationSaveRequest req, User actor) {
//        Consultation c = own(id, actor);
//        if (c.getStatus() != ConsultationStatus.IN_PROGRESS) throw ApiException.conflict("Consultation is already completed.");
//        apply(c, req);
//        return mapper.consultation(c);
//    }
//
//    private void apply(Consultation c, ConsultationSaveRequest req) {
//        c.setSymptoms(req.symptoms());
//        c.setExaminationNotes(req.examinationNotes());
//        c.setTreatmentPlan(req.treatmentPlan());
//        c.setTestsAdvised(req.testsAdvised());
//        c.setAdvice(req.advice());
//        String lang = req.language() == null ? "EN" : req.language();
//        c.setLanguage(lang);
//        boolean local = !"EN".equals(lang);
//        c.setAdviceLocal(local ? blankToNull(req.adviceLocal()) : null);
//        c.setTestsAdvisedLocal(local ? blankToNull(req.testsAdvisedLocal()) : null);
//        c.getDiagnoses().clear();
//        if (req.diagnoses() != null) {
//            for (DiagnosisDto dto : req.diagnoses()) {
//                Diagnosis d = new Diagnosis();
//                d.setConsultation(c);
//                d.setName(dto.name());
//                d.setIcdCode(dto.icdCode());
//                d.setNotes(dto.notes());
//                c.getDiagnoses().add(d);
//            }
//        }
//        consultations.save(c);
//        Prescription rx = prescriptions.findByConsultationId(c.getId()).orElse(null);
//        boolean hasItems = req.items() != null && !req.items().isEmpty();
//        if (!hasItems) {
//            if (rx != null) prescriptions.delete(rx);
//            return;
//        }
//        if (rx == null) {
//            rx = new Prescription();
//            rx.setPrescriptionCode("TMP-" + UUID.randomUUID().toString().substring(0, 20));
//            rx.setConsultation(c);
//            rx.setPatient(c.getPatient());
//            rx.setDoctor(c.getDoctor());
//            rx = prescriptions.saveAndFlush(rx);
//            rx.setPrescriptionCode(Codes.of("RX", rx.getId()));
//        }
//        rx.setNotes(req.prescriptionNotes());
//        rx.getItems().clear();
//        for (PrescriptionItemDto dto : req.items()) {
//            PrescriptionItem i = new PrescriptionItem();
//            i.setPrescription(rx);
//            i.setMedicineName(dto.medicineName());
//            i.setComposition(dto.composition() == null || dto.composition().isBlank() ? null : dto.composition().trim());
//            i.setDosage(dto.dosage());
//            i.setFrequency(dto.frequency());
//            i.setDuration(dto.duration());
//            i.setInstructions(dto.instructions());
//            i.setFrequencyLocal(local ? blankToNull(dto.frequencyLocal()) : null);
//            i.setDurationLocal(local ? blankToNull(dto.durationLocal()) : null);
//            i.setInstructionsLocal(local ? blankToNull(dto.instructionsLocal()) : null);
//            rx.getItems().add(i);
//            medicines.remember(dto, false);
//        }
//        prescriptions.save(rx);
//    }
//
//    private static String blankToNull(String v) { return v == null || v.isBlank() ? null : v.trim(); }
//
//    /** Completes consultation, issues prescription, creates the follow-up appointment/reminders. */
//    @Transactional
//    public CompleteResponse complete(Long id, CompleteRequest req, User actor) {
//        Consultation c = own(id, actor);
//        if (c.getStatus() != ConsultationStatus.IN_PROGRESS) throw ApiException.conflict("Consultation is already completed.");
//        if (req.consultation() != null) apply(c, req.consultation());
//        if (c.getDiagnoses().isEmpty()) throw ApiException.badRequest("Add at least one diagnosis before completing the consultation.");
//        c.setStatus(ConsultationStatus.COMPLETED);
//        c.setCompletedAt(LocalDateTime.now());
//        Appointment a = c.getAppointment();
//        a.setStatus(AppointmentStatus.COMPLETED);
//        queues.findByAppointmentId(a.getId()).ifPresent(q -> { q.setStatus(QueueStatus.COMPLETED); q.setCompletedAt(LocalDateTime.now()); });
//        consultations.save(c);
//
//        prescriptions.findByConsultationId(c.getId()).ifPresent(rx -> {
//            notifications.notifyPrescriptionAvailable(rx);
//            rx.getItems().forEach(i -> medicines.remember(new PrescriptionItemDto(i.getMedicineName(), i.getComposition(), i.getDosage(), i.getFrequency(), i.getDuration(), i.getInstructions(), null, null, null), true));
//        });
//
//        FollowUp f = createFollowUp(c, req.followUp(), req.customDate(), req.customTime(), actor);
//        String msg = f == null ? "Consultation completed. No follow-up requested."
//                : f.getStatus() == FollowUpStatus.SCHEDULED
//                    ? "Follow-up appointment booked for " + f.getFollowUpDate() + " at " + f.getFollowUpTime() + ". Reminders are scheduled."
//                    : "Follow-up recorded for " + f.getFollowUpDate() + ", but no free slot could be booked automatically. The patient will be reminded and reception should book a time.";
//        audit.log("CONSULTATION_COMPLETED", "Consultation", c.getId(), "followUp=" + req.followUp());
//        return new CompleteResponse(mapper.consultation(c), f == null ? null : mapper.followUp(f), msg);
//    }
//
//    private FollowUp createFollowUp(Consultation c, FollowUpOption option, LocalDate customDate, LocalTime customTime, User actor) {
//        LocalDate target;
//        try {
//            target = FollowUpCalc.targetDate(LocalDate.now(), option, customDate);
//        } catch (IllegalArgumentException e) {
//            throw ApiException.badRequest(e.getMessage());
//        }
//        if (target == null) return null;
//        Doctor doctor = c.getDoctor();
//        LocalTime preferred = customTime != null ? customTime : c.getAppointment().getStartTime();
//
//        LocalDate chosenDate = target;
//        Optional<SlotView> slot;
//        if (option == FollowUpOption.CUSTOM) {
//            slot = availability.findOpenSlot(doctor, target, preferred, false, null).or(() -> availability.nearestOnDate(doctor, target, preferred));
//        } else {
//            slot = Optional.empty();
//            for (int i = 0; i < 14 && slot.isEmpty(); i++) { // roll forward to the next working day with a free slot
//                chosenDate = target.plusDays(i);
//                slot = availability.nearestOnDate(doctor, chosenDate, preferred);
//            }
//        }
//        FollowUp f = new FollowUp();
//        f.setConsultation(c);
//        f.setPatient(c.getPatient());
//        f.setDoctor(doctor);
//        if (slot.isPresent()) {
//            Appointment fa = appointmentService.create(c.getPatient(), doctor.getId(), chosenDate, slot.get().startTime(),
//                    "Follow-up visit", AppointmentType.FOLLOW_UP, AppointmentStatus.CONFIRMED, actor);
//            f.setAppointment(fa);
//            f.setFollowUpDate(chosenDate);
//            f.setFollowUpTime(slot.get().startTime());
//            f.setStatus(FollowUpStatus.SCHEDULED);
//            followUps.save(f);
//        } else {
//            f.setFollowUpDate(target);
//            f.setFollowUpTime(customTime);
//            f.setStatus(FollowUpStatus.PENDING);
//            followUps.save(f);
//            notifications.scheduleForPendingFollowUp(f);
//        }
//        return f;
//    }
//
//    // ---------- history & portal reads ----------
//
//    public HistoryView history(Long patientId, User actor) {
//        Patient p = access.requirePatient(actor, patientId);
//        if (actor.getRole() != Role.DOCTOR) throw ApiException.forbidden("Clinical history is available to the treating doctor only");
//        audit.log("PATIENT_HISTORY_VIEWED", "Patient", patientId, null);
//        return new HistoryView(mapper.patient(p),
//                appointments.findByPatientIdOrderByAppointmentDateDescStartTimeDesc(patientId).stream().map(mapper::appointment).toList(),
//                consultations.findByPatientIdOrderByStartedAtDesc(patientId).stream().map(mapper::consultation).toList(),
//                prescriptions.findByPatientIdOrderByCreatedAtDesc(patientId).stream().map(mapper::prescription).toList(),
//                followUps.findByPatientIdOrderByFollowUpDateDesc(patientId).stream().map(mapper::followUp).toList(),
//                reports.findByPatientIdOrderByReportDateDescIdDesc(patientId).stream().map(mapper::report).toList(),
//                bills.findByPatientIdOrderByCreatedAtDesc(patientId).stream().map(mapper::bill).toList());
//    }
//
//    /** Patient portal: completed visits with what the doctor recorded and advised. */
//    public List<ConsultationView> myVisits(User actor) {
//        Patient p = access.patientOf(actor);
//        return consultations.findByPatientIdOrderByStartedAtDesc(p.getId()).stream()
//                .filter(c -> c.getStatus() == ConsultationStatus.COMPLETED).map(mapper::consultation).toList();
//    }
//
//    public List<PrescriptionView> myPrescriptions(User actor) {
//        Patient p = access.patientOf(actor);
//        return prescriptions.findByPatientIdOrderByCreatedAtDesc(p.getId()).stream().map(mapper::prescription).toList();
//    }
//
//    public List<FollowUpView> myFollowUps(User actor) {
//        Patient p = access.patientOf(actor);
//        return followUps.findByPatientIdOrderByFollowUpDateDesc(p.getId()).stream().map(mapper::followUp).toList();
//    }
//
//    public Prescription requirePrescription(Long id, User actor) {
//        Prescription rx = prescriptions.findById(id).orElseThrow(() -> ApiException.notFound("Prescription not found"));
//        switch (actor.getRole()) {
//            case PATIENT -> { if (rx.getPatient().getUser() == null || !rx.getPatient().getUser().getId().equals(actor.getId())) throw ApiException.forbidden("Access denied"); }
//            case DOCTOR -> { if (!access.doctorTreats(actor, rx.getPatient().getId())) throw ApiException.forbidden("Access denied"); }
//            case ADMIN -> throw ApiException.forbidden("Access denied");
//            default -> { }
//        }
//        audit.log("PRESCRIPTION_VIEWED", "Prescription", id, null);
//        return rx;
//    }
//
//    public PrescriptionView prescription(Long id, User actor) {
//        return mapper.prescription(requirePrescription(id, actor));
//    }
//}

package com.munot.hospital.service;

import com.munot.hospital.common.ApiException;
import com.munot.hospital.dto.AppointmentDtos.SlotView;
import com.munot.hospital.dto.BillingDtos.BillView;
import com.munot.hospital.dto.ClinicalDtos.*;
import com.munot.hospital.entity.*;
import com.munot.hospital.repository.*;
import com.munot.hospital.util.Codes;
import com.munot.hospital.util.FollowUpCalc;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConsultationService {
    private final ConsultationRepository consultations;
    private final PrescriptionRepository prescriptions;
    private final FollowUpRepository followUps;
    private final QueueEntryRepository queues;
    private final AppointmentRepository appointments;
    private final MedicalReportRepository reports;
    private final BillRepository bills;
    private final AppointmentService appointmentService;
    private final AvailabilityService availability;
    private final NotificationService notifications;
    private final MedicineService medicines;
    private final AccessControl access;
    private final ViewMapper mapper;
    private final AuditService audit;

    // ---------- consultation lifecycle ----------

    /** Doctor calls a waiting patient in: creates the consultation and moves queue + appointment to IN_CONSULTATION. */
    @Transactional
    public ConsultationView start(Long appointmentId, User actor) {
        Appointment a = access.requireAppointment(actor, appointmentId);
        Doctor doctor = access.doctorOf(actor);
        Consultation existing = consultations.findByAppointmentId(a.getId()).orElse(null);
        if (existing != null) return mapper.consultation(existing);
        if (a.getStatus() != AppointmentStatus.WAITING) throw ApiException.conflict("Patient must be checked in and waiting (current: " + a.getStatus() + ").");
        if (queues.existsByDoctorIdAndQueueDateAndStatus(doctor.getId(), a.getAppointmentDate(), QueueStatus.IN_CONSULTATION)) {
            throw ApiException.conflict("Please complete the current consultation before calling the next patient.");
        }
        QueueEntry q = queues.findByAppointmentId(a.getId()).orElseThrow(() -> ApiException.conflict("Patient is not in the queue"));
        Consultation c = new Consultation();
        c.setAppointment(a);
        c.setPatient(a.getPatient());
        c.setDoctor(doctor);
        c.setStatus(ConsultationStatus.IN_PROGRESS);
        c.setStartedAt(LocalDateTime.now());
        consultations.save(c);
        q.setStatus(QueueStatus.IN_CONSULTATION);
        q.setCalledAt(LocalDateTime.now());
        a.setStatus(AppointmentStatus.IN_CONSULTATION);
        audit.log("CONSULTATION_STARTED", "Consultation", c.getId(), a.getAppointmentCode());
        return mapper.consultation(c);
    }

    private Consultation own(Long id, User actor) {
        Consultation c = consultations.findById(id).orElseThrow(() -> ApiException.notFound("Consultation not found"));
        if (actor.getRole() != Role.DOCTOR || !c.getDoctor().getUser().getId().equals(actor.getId())) throw ApiException.forbidden("Access denied");
        return c;
    }

    public ConsultationView get(Long id, User actor) {
        Consultation c = own(id, actor);
        audit.log("CONSULTATION_VIEWED", "Consultation", id, null);
        return mapper.consultation(c);
    }

    @Transactional
    public ConsultationView save(Long id, ConsultationSaveRequest req, User actor) {
        Consultation c = own(id, actor);
        boolean completed = c.getStatus() == ConsultationStatus.COMPLETED;
        if (completed) {
            // Editing a completed consultation / prescription: same owner-only check as above, but the record must stay valid and
            // nothing else is touched (appointment, queue, follow-up and billing are not changed by apply()).
            boolean hasDiagnosis = req.diagnoses() != null && req.diagnoses().stream().anyMatch(d -> d.name() != null && !d.name().isBlank());
            if (!hasDiagnosis) throw ApiException.badRequest("A completed consultation must keep at least one diagnosis.");
            boolean noItems = req.items() == null || req.items().isEmpty();
            if (noItems && prescriptions.findByConsultationId(c.getId()).isPresent()) {
                throw ApiException.badRequest("An issued prescription must keep at least one medicine.");
            }
        } else if (c.getStatus() != ConsultationStatus.IN_PROGRESS) {
            throw ApiException.conflict("Consultation is not editable.");
        }
        apply(c, req);
        if (completed) audit.log("PRESCRIPTION_EDITED", "Consultation", id, "edited after completion");
        return mapper.consultation(c);
    }

    private void apply(Consultation c, ConsultationSaveRequest req) {
        c.setSymptoms(req.symptoms());
        c.setExaminationNotes(req.examinationNotes());
        c.setTreatmentPlan(req.treatmentPlan());
        c.setTestsAdvised(req.testsAdvised());
        c.setAdvice(req.advice());
        String lang = req.language() == null ? "EN" : req.language();
        c.setLanguage(lang);
        boolean local = !"EN".equals(lang);
        c.setAdviceLocal(local ? blankToNull(req.adviceLocal()) : null);
        c.setTestsAdvisedLocal(local ? blankToNull(req.testsAdvisedLocal()) : null);
        c.getDiagnoses().clear();
        if (req.diagnoses() != null) {
            for (DiagnosisDto dto : req.diagnoses()) {
                Diagnosis d = new Diagnosis();
                d.setConsultation(c);
                d.setName(dto.name());
                d.setIcdCode(dto.icdCode());
                d.setNotes(dto.notes());
                c.getDiagnoses().add(d);
            }
        }
        consultations.save(c);
        Prescription rx = prescriptions.findByConsultationId(c.getId()).orElse(null);
        boolean hasItems = req.items() != null && !req.items().isEmpty();
        if (!hasItems) {
            if (rx != null) prescriptions.delete(rx);
            return;
        }
        if (rx == null) {
            rx = new Prescription();
            rx.setPrescriptionCode("TMP-" + UUID.randomUUID().toString().substring(0, 20));
            rx.setConsultation(c);
            rx.setPatient(c.getPatient());
            rx.setDoctor(c.getDoctor());
            rx = prescriptions.saveAndFlush(rx);
            rx.setPrescriptionCode(Codes.of("RX", rx.getId()));
        }
        rx.setNotes(req.prescriptionNotes());
        rx.getItems().clear();
        for (PrescriptionItemDto dto : req.items()) {
            PrescriptionItem i = new PrescriptionItem();
            i.setPrescription(rx);
            i.setMedicineName(dto.medicineName());
            i.setComposition(dto.composition() == null || dto.composition().isBlank() ? null : dto.composition().trim());
            i.setDosage(dto.dosage());
            i.setFrequency(dto.frequency());
            i.setDuration(dto.duration());
            i.setInstructions(dto.instructions());
            i.setFrequencyLocal(local ? blankToNull(dto.frequencyLocal()) : null);
            i.setDurationLocal(local ? blankToNull(dto.durationLocal()) : null);
            i.setInstructionsLocal(local ? blankToNull(dto.instructionsLocal()) : null);
            rx.getItems().add(i);
            medicines.remember(dto, false);
        }
        prescriptions.save(rx);
    }

    private static String blankToNull(String v) { return v == null || v.isBlank() ? null : v.trim(); }

    /** Completes consultation, issues prescription, creates the follow-up appointment/reminders. */
    @Transactional
    public CompleteResponse complete(Long id, CompleteRequest req, User actor) {
        Consultation c = own(id, actor);
        if (c.getStatus() != ConsultationStatus.IN_PROGRESS) throw ApiException.conflict("Consultation is already completed.");
        if (req.consultation() != null) apply(c, req.consultation());
        if (c.getDiagnoses().isEmpty()) throw ApiException.badRequest("Add at least one diagnosis before completing the consultation.");
        c.setStatus(ConsultationStatus.COMPLETED);
        c.setCompletedAt(LocalDateTime.now());
        Appointment a = c.getAppointment();
        a.setStatus(AppointmentStatus.COMPLETED);
        queues.findByAppointmentId(a.getId()).ifPresent(q -> { q.setStatus(QueueStatus.COMPLETED); q.setCompletedAt(LocalDateTime.now()); });
        consultations.save(c);

        prescriptions.findByConsultationId(c.getId()).ifPresent(rx -> {
            notifications.notifyPrescriptionAvailable(rx);
            rx.getItems().forEach(i -> medicines.remember(new PrescriptionItemDto(i.getMedicineName(), i.getComposition(), i.getDosage(), i.getFrequency(), i.getDuration(), i.getInstructions(), null, null, null), true));
        });

        FollowUp f = createFollowUp(c, req.followUp(), req.customDate(), req.customTime(), actor);
        String msg = f == null ? "Consultation completed. No follow-up requested."
                : f.getStatus() == FollowUpStatus.SCHEDULED
                ? "Follow-up appointment booked for " + f.getFollowUpDate() + " at " + f.getFollowUpTime() + ". Reminders are scheduled."
                : "Follow-up recorded for " + f.getFollowUpDate() + ", but no free slot could be booked automatically. The patient will be reminded and reception should book a time.";
        audit.log("CONSULTATION_COMPLETED", "Consultation", c.getId(), "followUp=" + req.followUp());
        return new CompleteResponse(mapper.consultation(c), f == null ? null : mapper.followUp(f), msg);
    }

    private FollowUp createFollowUp(Consultation c, FollowUpOption option, LocalDate customDate, LocalTime customTime, User actor) {
        LocalDate target;
        try {
            target = FollowUpCalc.targetDate(LocalDate.now(), option, customDate);
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest(e.getMessage());
        }
        if (target == null) return null;
        Doctor doctor = c.getDoctor();
        LocalTime preferred = customTime != null ? customTime : c.getAppointment().getStartTime();

        LocalDate chosenDate = target;
        Optional<SlotView> slot;
        if (option == FollowUpOption.CUSTOM) {
            slot = availability.findOpenSlot(doctor, target, preferred, false, null).or(() -> availability.nearestOnDate(doctor, target, preferred));
        } else {
            slot = Optional.empty();
            for (int i = 0; i < 14 && slot.isEmpty(); i++) { // roll forward to the next working day with a free slot
                chosenDate = target.plusDays(i);
                slot = availability.nearestOnDate(doctor, chosenDate, preferred);
            }
        }
        FollowUp f = new FollowUp();
        f.setConsultation(c);
        f.setPatient(c.getPatient());
        f.setDoctor(doctor);
        if (slot.isPresent()) {
            Appointment fa = appointmentService.create(c.getPatient(), doctor.getId(), chosenDate, slot.get().startTime(),
                    "Follow-up visit", AppointmentType.FOLLOW_UP, AppointmentStatus.CONFIRMED, actor);
            f.setAppointment(fa);
            f.setFollowUpDate(chosenDate);
            f.setFollowUpTime(slot.get().startTime());
            f.setStatus(FollowUpStatus.SCHEDULED);
            followUps.save(f);
        } else {
            f.setFollowUpDate(target);
            f.setFollowUpTime(customTime);
            f.setStatus(FollowUpStatus.PENDING);
            followUps.save(f);
            notifications.scheduleForPendingFollowUp(f);
        }
        return f;
    }

    // ---------- history & portal reads ----------

    public HistoryView history(Long patientId, User actor) {
        Patient p = access.requirePatient(actor, patientId);
        if (actor.getRole() != Role.DOCTOR) throw ApiException.forbidden("Clinical history is available to the treating doctor only");
        audit.log("PATIENT_HISTORY_VIEWED", "Patient", patientId, null);
        return new HistoryView(mapper.patient(p),
                appointments.findByPatientIdOrderByAppointmentDateDescStartTimeDesc(patientId).stream().map(mapper::appointment).toList(),
                consultations.findByPatientIdOrderByStartedAtDesc(patientId).stream().map(mapper::consultation).toList(),
                prescriptions.findByPatientIdOrderByCreatedAtDesc(patientId).stream().map(mapper::prescription).toList(),
                followUps.findByPatientIdOrderByFollowUpDateDesc(patientId).stream().map(mapper::followUp).toList(),
                reports.findByPatientIdOrderByReportDateDescIdDesc(patientId).stream().map(mapper::report).toList(),
                bills.findByPatientIdOrderByCreatedAtDesc(patientId).stream().map(mapper::bill).toList());
    }

    /** Patient portal: completed visits with what the doctor recorded and advised. */
    public List<ConsultationView> myVisits(User actor) {
        Patient p = access.patientOf(actor);
        return consultations.findByPatientIdOrderByStartedAtDesc(p.getId()).stream()
                .filter(c -> c.getStatus() == ConsultationStatus.COMPLETED).map(mapper::consultation).toList();
    }

    public List<PrescriptionView> myPrescriptions(User actor) {
        Patient p = access.patientOf(actor);
        return prescriptions.findByPatientIdOrderByCreatedAtDesc(p.getId()).stream().map(mapper::prescription).toList();
    }

    public List<FollowUpView> myFollowUps(User actor) {
        Patient p = access.patientOf(actor);
        return followUps.findByPatientIdOrderByFollowUpDateDesc(p.getId()).stream().map(mapper::followUp).toList();
    }

    public Prescription requirePrescription(Long id, User actor) {
        Prescription rx = prescriptions.findById(id).orElseThrow(() -> ApiException.notFound("Prescription not found"));
        switch (actor.getRole()) {
            case PATIENT -> { if (rx.getPatient().getUser() == null || !rx.getPatient().getUser().getId().equals(actor.getId())) throw ApiException.forbidden("Access denied"); }
            case DOCTOR -> { if (!access.doctorTreats(actor, rx.getPatient().getId())) throw ApiException.forbidden("Access denied"); }
            case ADMIN -> throw ApiException.forbidden("Access denied");
            default -> { }
        }
        audit.log("PRESCRIPTION_VIEWED", "Prescription", id, null);
        return rx;
    }

    public PrescriptionView prescription(Long id, User actor) {
        return mapper.prescription(requirePrescription(id, actor));
    }
}