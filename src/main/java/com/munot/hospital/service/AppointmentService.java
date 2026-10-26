package com.munot.hospital.service;

import com.munot.hospital.common.ApiException;
import com.munot.hospital.dto.AppointmentDtos.*;
import com.munot.hospital.entity.*;
import com.munot.hospital.repository.*;
import com.munot.hospital.security.CurrentUser;
import com.munot.hospital.util.AppointmentRules;
import com.munot.hospital.util.Codes;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AppointmentService {
    private static final Logger log = LoggerFactory.getLogger(AppointmentService.class);
    private static final List<AppointmentStatus> OPEN = List.of(AppointmentStatus.BOOKED, AppointmentStatus.CONFIRMED);

    private final AppointmentRepository appointments;
    private final DoctorRepository doctors;
    private final PatientRepository patients;
    private final QueueEntryRepository queues;
    private final AvailabilityService availability;
    private final NotificationService notifications;
    private final AccessControl access;
    private final SettingsService settings;
    private final ViewMapper mapper;
    private final AuditService audit;

    // ---------- booking ----------

    @Transactional
    public AppointmentView book(BookRequest req, User actor) {
        Patient patient;
        boolean staff = actor.getRole() == Role.RECEPTIONIST || actor.getRole() == Role.ADMIN;
        if (actor.getRole() == Role.PATIENT) {
            patient = access.patientOf(actor);
        } else if (staff) {
            if (req.patientId() == null) throw ApiException.badRequest("patientId is required");
            patient = patients.findById(req.patientId()).orElseThrow(() -> ApiException.notFound("Patient not found"));
        } else {
            throw ApiException.forbidden("Not allowed to book appointments");
        }
        if (actor.getRole() == Role.PATIENT) {
            int maxAdvance = settings.getInt("max_advance_days", 60);
            if (!AppointmentRules.withinAdvanceLimit(LocalDate.now(), req.date(), maxAdvance))
                throw ApiException.badRequest("Appointments can be booked up to " + maxAdvance + " days in advance.");
            int maxActive = settings.getInt("max_active_appointments", 5);
            if (appointments.countByPatientIdAndStatusInAndAppointmentDateGreaterThanEqual(patient.getId(), OPEN, LocalDate.now()) >= maxActive)
                throw ApiException.conflict("You already have " + maxActive + " upcoming appointments. Please attend or cancel one before booking another.");
        }
        AppointmentType type = staff && req.type() != null ? req.type() : (staff ? AppointmentType.WALK_IN : AppointmentType.ONLINE);
        if (type == AppointmentType.FOLLOW_UP && !staff) type = AppointmentType.ONLINE;
        Appointment a = create(patient, req.doctorId(), req.date(), req.startTime(), req.reason(), type,
                AppointmentRules.needsStaffConfirmation(actor.getRole()) ? AppointmentStatus.BOOKED : AppointmentStatus.CONFIRMED, actor);
        audit.log("APPOINTMENT_BOOKED", "Appointment", a.getId(), a.getAppointmentCode() + " " + type);
        return mapper.appointment(a);
    }

    /** Core creation used for online, walk-in and follow-up appointments. Locks the doctor row to prevent double booking. */
    @Transactional
    public Appointment create(Patient patient, Long doctorId, LocalDate date, LocalTime start, String reason, AppointmentType type,
                              AppointmentStatus status, User creator) {
        Doctor doctor = doctors.findByIdForUpdate(doctorId).orElseThrow(() -> ApiException.notFound("Doctor not found"));
        boolean walkIn = type == AppointmentType.WALK_IN;
        SlotView slot = availability.findOpenSlot(doctor, date, start, walkIn, null)
                .orElseThrow(() -> ApiException.conflict("That time slot is not available. Please choose another slot."));
        if (appointments.existsByPatientIdAndAppointmentDateAndStartTimeAndStatusNotIn(patient.getId(), date, start, AvailabilityService.INACTIVE)) {
            throw ApiException.conflict("This patient already has an appointment at that time.");
        }
        Appointment a = new Appointment();
        a.setAppointmentCode("TMP-" + UUID.randomUUID().toString().substring(0, 24));
        a.setPatient(patient);
        a.setDoctor(doctor);
        a.setAppointmentDate(date);
        a.setStartTime(slot.startTime());
        a.setEndTime(slot.endTime());
        a.setStatus(status);
        a.setType(type);
        a.setReason(reason);
        a.setCreatedBy(creator);
        a = appointments.saveAndFlush(a);
        a.setAppointmentCode(Codes.of("APT", a.getId()));
        appointments.save(a);
        notifications.scheduleForAppointment(a);
        return a;
    }

    @Transactional
    public AppointmentView reschedule(Long id, RescheduleRequest req, User actor) {
        Appointment a = access.requireAppointment(actor, id);
        requireModifiable(a, actor);
        Doctor doctor = doctors.findByIdForUpdate(a.getDoctor().getId()).orElseThrow();
        SlotView slot = availability.findOpenSlot(doctor, req.date(), req.startTime(), false, a.getId())
                .orElseThrow(() -> ApiException.conflict("That time slot is not available. Please choose another slot."));
        if (appointments.existsByPatientIdAndAppointmentDateAndStartTimeAndStatusNotIn(a.getPatient().getId(), req.date(), req.startTime(), AvailabilityService.INACTIVE)
                && !(a.getAppointmentDate().equals(req.date()) && a.getStartTime().equals(req.startTime()))) {
            throw ApiException.conflict("This patient already has an appointment at that time.");
        }
        a.setAppointmentDate(req.date());
        a.setStartTime(slot.startTime());
        a.setEndTime(slot.endTime());
        a.setStatus(AppointmentRules.statusAfterReschedule(a.getStatus(), actor.getRole()));
        appointments.save(a);
        notifications.cancelPending(a.getId());
        notifications.scheduleForAppointment(a);
        audit.log("APPOINTMENT_RESCHEDULED", "Appointment", a.getId(), req.date() + " " + req.startTime());
        return mapper.appointment(a);
    }

    @Transactional
    public AppointmentView cancel(Long id, String reason, User actor) {
        Appointment a = access.requireAppointment(actor, id);
        requireModifiable(a, actor);
        a.setStatus(AppointmentStatus.CANCELLED);
        a.setCancelReason(reason);
        appointments.save(a);
        notifications.cancelPending(a.getId());
        if (actor.getRole() != Role.PATIENT) notifications.notifyCancelled(a);
        audit.log("APPOINTMENT_CANCELLED", "Appointment", a.getId(), reason);
        return mapper.appointment(a);
    }

    private void requireModifiable(Appointment a, User actor) {
        if (!OPEN.contains(a.getStatus())) throw ApiException.conflict("Only booked or confirmed appointments can be changed.");
        if (actor.getRole() == Role.PATIENT) {
            int cutoff = settings.getInt("cancellation_cutoff_hours", 2);
            if (!LocalDateTime.of(a.getAppointmentDate(), a.getStartTime()).isAfter(LocalDateTime.now().plusHours(cutoff))) {
                throw ApiException.conflict("Changes are not allowed within " + cutoff + " hours of the appointment. Please call the hospital.");
            }
        }
    }

    /** Doctor or reception accepts an online request. Only now does it become CONFIRMED and reminders are scheduled. */
    @Transactional
    public AppointmentView confirm(Long id, User actor) {
        Appointment a = access.requireAppointment(actor, id);
        if (!AppointmentRules.canConfirm(a.getStatus())) throw ApiException.conflict("Only appointments awaiting confirmation can be confirmed.");
        if (LocalDateTime.of(a.getAppointmentDate(), a.getStartTime()).isBefore(LocalDateTime.now())) throw ApiException.conflict("This appointment time has already passed.");
        a.setStatus(AppointmentStatus.CONFIRMED);
        appointments.save(a);
        notifications.cancelPending(a.getId());
        notifications.scheduleForAppointment(a);
        audit.log("APPOINTMENT_CONFIRMED", "Appointment", a.getId(), "by " + actor.getRole());
        return mapper.appointment(a);
    }

    /** Online requests still waiting for confirmation (doctors see only their own). */
    public List<AppointmentView> pending(User actor) {
        Long docId = actor.getRole() == Role.DOCTOR ? access.doctorOf(actor).getId() : null;
        return appointments.findByStatusAndAppointmentDateGreaterThanEqualOrderByAppointmentDateAscStartTimeAsc(AppointmentStatus.BOOKED, LocalDate.now()).stream()
                .filter(a -> docId == null || a.getDoctor().getId().equals(docId)).map(mapper::appointment).toList();
    }

    @Transactional
    public AppointmentView markNoShow(Long id) {
        Appointment a = appointments.findById(id).orElseThrow(() -> ApiException.notFound("Appointment not found"));
        if (!OPEN.contains(a.getStatus())) throw ApiException.conflict("Only booked or confirmed appointments can be marked no-show.");
        a.setStatus(AppointmentStatus.NO_SHOW);
        notifications.cancelPending(a.getId());
        audit.log("APPOINTMENT_NO_SHOW", "Appointment", a.getId(), null);
        return mapper.appointment(a);
    }

    // ---------- check-in & queue ----------

    @Transactional
    public QueueView checkIn(Long id) {
        Appointment a = appointments.findById(id).orElseThrow(() -> ApiException.notFound("Appointment not found"));
        if (!AppointmentRules.canCheckIn(a.getStatus())) throw ApiException.conflict("Only booked or confirmed appointments can be checked in (current: " + a.getStatus() + ").");
        if (!a.getAppointmentDate().equals(LocalDate.now())) throw ApiException.conflict("Check-in is only possible on the appointment date.");
        doctors.findByIdForUpdate(a.getDoctor().getId()); // serialises token generation per doctor
        if (queues.findByAppointmentId(a.getId()).isPresent()) throw ApiException.conflict("Patient is already checked in.");
        QueueEntry q = new QueueEntry();
        q.setAppointment(a);
        q.setDoctor(a.getDoctor());
        q.setQueueDate(a.getAppointmentDate());
        q.setTokenNumber(queues.maxToken(a.getDoctor().getId(), a.getAppointmentDate()) + 1);
        q.setStatus(QueueStatus.WAITING);
        q.setCheckedInAt(LocalDateTime.now());
        queues.save(q);
        a.setStatus(AppointmentStatus.WAITING); // CHECKED_IN -> token issued -> WAITING happen in one step
        notifications.cancelPending(a.getId());
        audit.log("PATIENT_CHECKED_IN", "Appointment", a.getId(), "token " + q.getTokenNumber());
        return mapper.queue(q);
    }

    public List<QueueView> queue(Long doctorId, LocalDate date, User actor) {
        LocalDate d = date == null ? LocalDate.now() : date;
        Long docId = doctorId;
        if (actor.getRole() == Role.DOCTOR) docId = access.doctorOf(actor).getId();
        List<QueueEntry> list = docId == null ? queues.findByQueueDateOrderByTokenNumber(d) : queues.findByDoctorIdAndQueueDateOrderByTokenNumber(docId, d);
        return list.stream().sorted(Comparator.comparing((QueueEntry q) -> q.getStatus() == QueueStatus.IN_CONSULTATION ? 0 : q.getStatus() == QueueStatus.WAITING ? 1 : 2)
                .thenComparing(QueueEntry::getTokenNumber)).map(mapper::queue).toList();
    }

    @Transactional
    public QueueView skip(Long appointmentId, User actor) {
        Appointment a = access.requireAppointment(actor, appointmentId);
        QueueEntry q = queues.findByAppointmentId(a.getId()).orElseThrow(() -> ApiException.notFound("Not in queue"));
        if (q.getStatus() != QueueStatus.WAITING) throw ApiException.conflict("Only waiting patients can be skipped.");
        q.setStatus(QueueStatus.SKIPPED);
        a.setStatus(AppointmentStatus.NO_SHOW);
        audit.log("QUEUE_SKIPPED", "Appointment", a.getId(), null);
        return mapper.queue(q);
    }

    // ---------- reads ----------

    public List<AppointmentView> mine(User actor) {
        Patient p = access.patientOf(actor);
        return appointments.findByPatientIdOrderByAppointmentDateDescStartTimeDesc(p.getId()).stream().map(mapper::appointment).toList();
    }

    public List<AppointmentView> list(LocalDate date, Long doctorId, AppointmentStatus status, User actor) {
        LocalDate d = date == null ? LocalDate.now() : date;
        Long docId = doctorId;
        if (actor.getRole() == Role.DOCTOR) docId = access.doctorOf(actor).getId();
        List<Appointment> list = docId == null ? appointments.findByAppointmentDateOrderByStartTime(d)
                : appointments.findByDoctorIdAndAppointmentDateOrderByStartTime(docId, d);
        return list.stream().filter(a -> status == null || a.getStatus() == status).map(mapper::appointment).toList();
    }

    public AppointmentView get(Long id, User actor) {
        return mapper.appointment(access.requireAppointment(actor, id));
    }

    public List<SlotView> slots(Long doctorId, LocalDate date, boolean walkIn) {
        Doctor d = doctors.findById(doctorId).orElseThrow(() -> ApiException.notFound("Doctor not found"));
        return availability.slots(d, date, walkIn, null);
    }

    /** Nightly clean-up: open appointments from past days become NO_SHOW. */
    @Scheduled(cron = "0 30 23 * * *")
    @Transactional
    public void markStaleAsNoShow() {
        List<Appointment> stale = appointments.findByAppointmentDateBeforeAndStatusIn(LocalDate.now(), OPEN);
        stale.forEach(a -> a.setStatus(AppointmentStatus.NO_SHOW));
        if (!stale.isEmpty()) log.info("Marked {} past appointments as NO_SHOW", stale.size());
    }
}
