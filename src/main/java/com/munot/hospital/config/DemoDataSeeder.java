package com.munot.hospital.config;

import com.munot.hospital.dto.BillingDtos.*;
import com.munot.hospital.entity.*;
import com.munot.hospital.repository.*;
import com.munot.hospital.service.BillingService;
import com.munot.hospital.service.NotificationService;
import com.munot.hospital.util.Codes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;

/**
 * Seeds FICTIONAL demo data when SEED_DEMO=true and the database has no users yet.
 * Never enable in production: it creates accounts with publicly documented passwords.
 */
@Component
public class DemoDataSeeder implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final AppProperties props;
    private final UserRepository users;
    private final DepartmentRepository departments;
    private final DoctorRepository doctors;
    private final ReceptionistRepository receptionists;
    private final PatientRepository patients;
    private final DoctorAvailabilityRepository availability;
    private final AppointmentRepository appointments;
    private final QueueEntryRepository queues;
    private final ConsultationRepository consultations;
    private final PrescriptionRepository prescriptions;
    private final FollowUpRepository followUps;
    private final BillingService billing;
    private final NotificationService notifications;
    private final PasswordEncoder encoder;

    public DemoDataSeeder(AppProperties props, UserRepository users, DepartmentRepository departments, DoctorRepository doctors,
                          ReceptionistRepository receptionists, PatientRepository patients, DoctorAvailabilityRepository availability,
                          AppointmentRepository appointments, QueueEntryRepository queues, ConsultationRepository consultations,
                          PrescriptionRepository prescriptions, FollowUpRepository followUps, BillingService billing,
                          NotificationService notifications, PasswordEncoder encoder) {
        this.props = props; this.users = users; this.departments = departments; this.doctors = doctors; this.receptionists = receptionists;
        this.patients = patients; this.availability = availability; this.appointments = appointments; this.queues = queues;
        this.consultations = consultations; this.prescriptions = prescriptions; this.followUps = followUps; this.billing = billing;
        this.notifications = notifications; this.encoder = encoder;
    }

    private User user(String name, String email, String phone, String pw, Role role) {
        User u = new User();
        u.setFullName(name); u.setEmail(email); u.setPhone(phone); u.setPasswordHash(encoder.encode(pw)); u.setRole(role);
        return users.save(u);
    }

    private Patient patient(String name, String phone, User u, LocalDate dob, String gender) {
        Patient p = new Patient();
        p.setPatientCode("TMP-" + phone); p.setFullName(name); p.setPhone(phone); p.setUser(u); p.setDateOfBirth(dob); p.setGender(gender);
        p.setEmail(u == null ? null : u.getEmail());
        p = patients.saveAndFlush(p);
        p.setPatientCode(Codes.of("PAT", p.getId()));
        return p;
    }

    private Appointment appt(Patient p, Doctor d, LocalDate date, LocalTime start, AppointmentStatus st, AppointmentType type, String reason) {
        Appointment a = new Appointment();
        a.setAppointmentCode("TMP-" + p.getId() + date + start);
        a.setPatient(p); a.setDoctor(d); a.setAppointmentDate(date); a.setStartTime(start); a.setEndTime(start.plusMinutes(15));
        a.setStatus(st); a.setType(type); a.setReason(reason);
        a = appointments.saveAndFlush(a);
        a.setAppointmentCode(Codes.of("APT", a.getId()));
        return a;
    }

    private static LocalDate workday(LocalDate d) { return d.getDayOfWeek() == DayOfWeek.SUNDAY ? d.plusDays(1) : d; }

    private static LocalDate workdayBack(LocalDate d) { return d.getDayOfWeek() == DayOfWeek.SUNDAY ? d.minusDays(1) : d; }

    @Override
    @Transactional
    public void run(String... args) {
        if (!props.seedDemo() || users.count() > 0) return;
        log.warn("SEED_DEMO=true: creating fictional demo data. Do NOT use this in production.");

        user("Demo Admin", "demo-admin@example.com", "9000000100", "Admin@12345", Role.ADMIN);
        User docUser = user("Dr. Demo Munot", "demo-doctor@example.com", "9000000101", "Doctor@12345", Role.DOCTOR);
        User recUser = user("Demo Receptionist", "demo-reception@example.com", "9000000102", "Reception@12345", Role.RECEPTIONIST);
        User patUser = user("Demo Patient", "demo-patient@example.com", "9000000001", "Patient@12345", Role.PATIENT);
        Receptionist rc = new Receptionist(); rc.setUser(recUser); receptionists.save(rc);

        Doctor doc = new Doctor();
        doc.setUser(docUser);
        doc.setDepartment(departments.findAll().stream().filter(d -> d.getName().equals("Orthopaedics")).findFirst().orElse(null));
        doc.setSpecialization("Orthopaedics (demo)");
        doc.setQualification("[Qualifications - update in Admin > Doctors]");
        doc.setConsultationFee(new BigDecimal("500.00"));
        doc.setBio("Demo profile. Replace with the doctor's real biography in Admin > Doctors.");
        doc = doctors.save(doc);
        for (DayOfWeek day : List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY)) {
            DoctorAvailability a = new DoctorAvailability();
            a.setDoctor(doc); a.setDayOfWeek(day); a.setStartTime(LocalTime.of(10, 0)); a.setEndTime(LocalTime.of(18, 0));
            a.setBreakStart(LocalTime.of(13, 0)); a.setBreakEnd(LocalTime.of(14, 0));
            availability.save(a);
        }

        Patient demo = patient("Demo Patient", "9000000001", patUser, LocalDate.of(1985, 5, 20), "Male");
        Patient asha = patient("Asha Demo", "9000000002", null, LocalDate.of(1972, 11, 3), "Female");
        Patient ravi = patient("Ravi Demo", "9000000003", null, LocalDate.of(1998, 2, 14), "Male");

        // --- completed visit in the past, with prescription, follow-up and a partly paid bill
        LocalDate past = workdayBack(LocalDate.now().minusDays(7));
        Appointment done = appt(demo, doc, past, LocalTime.of(10, 30), AppointmentStatus.COMPLETED, AppointmentType.ONLINE, "Lower back pain");
        Consultation c = new Consultation();
        c.setAppointment(done); c.setPatient(demo); c.setDoctor(doc); c.setStatus(ConsultationStatus.COMPLETED);
        c.setStartedAt(past.atTime(10, 35)); c.setCompletedAt(past.atTime(10, 50));
        c.setSymptoms("Lower back pain for two weeks, worse on bending (demo data)");
        c.setExaminationNotes("Mild tenderness over lumbar region. No neurological deficit (demo data)");
        c.setTreatmentPlan("Rest, physiotherapy exercises, review after one week (demo data)");
        c.setAdvice("Avoid heavy lifting. Hot fomentation twice a day.");
        c.setTestsAdvised("X-ray lumbar spine (demo)");
        Diagnosis dx = new Diagnosis(); dx.setConsultation(c); dx.setName("Lumbar strain (demo)"); c.getDiagnoses().add(dx);
        c = consultations.saveAndFlush(c);
        Prescription rx = new Prescription();
        rx.setPrescriptionCode("TMP-DEMO"); rx.setConsultation(c); rx.setPatient(demo); rx.setDoctor(doc);
        rx = prescriptions.saveAndFlush(rx);
        rx.setPrescriptionCode(Codes.of("RX", rx.getId()));
        String[][] meds = {{"Demo-Pain Tablet", "Paracetamol 500 mg", "1 tablet", "Twice daily", "5 days", "After food"},
                {"Demo-Calcium Tablet", "Calcium 500 mg + Vitamin D3 250 IU", "1 tablet", "Once daily", "30 days", ""}};
        for (String[] m : meds) {
            PrescriptionItem i = new PrescriptionItem();
            i.setPrescription(rx); i.setMedicineName(m[0]); i.setComposition(m[1]); i.setDosage(m[2]); i.setFrequency(m[3]); i.setDuration(m[4]); i.setInstructions(m[5]);
            rx.getItems().add(i);
        }
        prescriptions.save(rx);
        QueueEntry qd = new QueueEntry();
        qd.setAppointment(done); qd.setDoctor(doc); qd.setQueueDate(past); qd.setTokenNumber(1); qd.setStatus(QueueStatus.COMPLETED);
        qd.setCheckedInAt(past.atTime(10, 20)); qd.setCalledAt(past.atTime(10, 35)); qd.setCompletedAt(past.atTime(10, 50));
        queues.save(qd);

        LocalDate fuDate = workday(LocalDate.now().plusDays(3));
        Appointment fuAppt = appt(demo, doc, fuDate, LocalTime.of(11, 0), AppointmentStatus.CONFIRMED, AppointmentType.FOLLOW_UP, "Follow-up visit");
        FollowUp fu = new FollowUp();
        fu.setConsultation(c); fu.setPatient(demo); fu.setDoctor(doc); fu.setAppointment(fuAppt); fu.setFollowUpDate(fuDate);
        fu.setFollowUpTime(LocalTime.of(11, 0)); fu.setStatus(FollowUpStatus.SCHEDULED);
        followUps.save(fu);
        notifications.scheduleForAppointment(fuAppt);
        notifications.notifyPrescriptionAvailable(rx);

        billing.create(new BillRequest(demo.getId(), done.getId(), List.of(
                new BillItemRequest(BillCategory.CONSULTATION, "Orthopaedic consultation (demo)", 1, new BigDecimal("500.00")),
                new BillItemRequest(BillCategory.DIAGNOSTIC, "X-ray lumbar spine (demo)", 1, new BigDecimal("300.00"))),
                BigDecimal.ZERO, BigDecimal.ZERO, "Demo bill", new PaymentRequest(new BigDecimal("500.00"), PaymentMethod.CASH, null)), recUser);

        // --- today's clinic: one patient already waiting, one confirmed, one booked online
        LocalDate today = LocalDate.now();
        Appointment waiting = appt(asha, doc, today, LocalTime.of(10, 0), AppointmentStatus.WAITING, AppointmentType.WALK_IN, "Knee pain");
        QueueEntry q = new QueueEntry();
        q.setAppointment(waiting); q.setDoctor(doc); q.setQueueDate(today); q.setTokenNumber(1); q.setStatus(QueueStatus.WAITING);
        q.setCheckedInAt(LocalDateTime.now());
        queues.save(q);
        appt(ravi, doc, today, LocalTime.of(10, 15), AppointmentStatus.CONFIRMED, AppointmentType.ONLINE, "Ankle sprain");
        appt(demo, doc, today, LocalTime.of(15, 0), AppointmentStatus.BOOKED, AppointmentType.ONLINE, "Routine review");
        log.info("Demo data created.");
    }
}
