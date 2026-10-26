package com.munot.hospital.service;

import com.munot.hospital.common.ApiException;
import com.munot.hospital.dto.AdminDtos.*;
import com.munot.hospital.dto.AppointmentDtos.*;
import com.munot.hospital.dto.AuthDtos.UserView;
import com.munot.hospital.entity.*;
import com.munot.hospital.repository.*;
import com.munot.hospital.util.Phones;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminService {
    private final UserRepository users;
    private final DoctorRepository doctors;
    private final ReceptionistRepository receptionists;
    private final DepartmentRepository departments;
    private final MedicalServiceRepository services;
    private final DoctorAvailabilityRepository availability;
    private final DoctorLeaveRepository leaves;
    private final PatientRepository patients;
    private final AppointmentRepository appointments;
    private final PaymentRepository payments;
    private final BillRepository bills;
    private final AuditLogRepository auditLogs;
    private final PasswordEncoder encoder;
    private final ViewMapper mapper;
    private final AuditService audit;

    private static final String PW_RULE = "^(?=.*[A-Za-z])(?=.*\\d).{8,72}$";

    private String requirePassword(String pw) {
        if (pw == null || !pw.matches(PW_RULE)) throw ApiException.badRequest("Password must be 8-72 characters with at least one letter and one digit");
        return encoder.encode(pw);
    }

    private User newUser(String name, String email, String phone, String password, Role role) {
        String e = email.trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(e)) throw ApiException.conflict("Email already in use");
        User u = new User();
        u.setFullName(name.trim());
        u.setEmail(e);
        u.setPhone(phone == null || phone.isBlank() ? null : Phones.normalize(phone));
        u.setPasswordHash(requirePassword(password));
        u.setRole(role);
        return users.save(u);
    }

    private void updateUser(User u, String name, String email, String phone, String password) {
        String e = email.trim().toLowerCase();
        users.findByEmailIgnoreCase(e).ifPresent(o -> { if (!o.getId().equals(u.getId())) throw ApiException.conflict("Email already in use"); });
        u.setFullName(name.trim());
        u.setEmail(e);
        u.setPhone(phone == null || phone.isBlank() ? null : Phones.normalize(phone));
        if (password != null && !password.isBlank()) u.setPasswordHash(requirePassword(password));
    }

    // ---------- doctors ----------

    public List<DoctorView> doctors(boolean activeOnly) {
        return (activeOnly ? doctors.findByActiveTrue() : doctors.findAll()).stream().map(mapper::doctor).toList();
    }

    @Transactional
    public DoctorView createDoctor(DoctorRequest r) {
        User u = newUser(r.fullName(), r.email(), r.phone(), r.password(), Role.DOCTOR);
        Doctor d = new Doctor();
        d.setUser(u);
        applyDoctor(d, r);
        d = doctors.save(d);
        audit.log("DOCTOR_CREATED", "Doctor", d.getId(), u.getEmail());
        return mapper.doctor(d);
    }

    @Transactional
    public DoctorView updateDoctor(Long id, DoctorRequest r) {
        Doctor d = doctors.findById(id).orElseThrow(() -> ApiException.notFound("Doctor not found"));
        updateUser(d.getUser(), r.fullName(), r.email(), r.phone(), r.password());
        applyDoctor(d, r);
        audit.log("DOCTOR_UPDATED", "Doctor", id, null);
        return mapper.doctor(d);
    }

    private void applyDoctor(Doctor d, DoctorRequest r) {
        d.setDepartment(r.departmentId() == null ? null : departments.findById(r.departmentId()).orElseThrow(() -> ApiException.badRequest("Unknown department")));
        d.setSpecialization(r.specialization());
        d.setQualification(r.qualification());
        d.setRegistrationNumber(r.registrationNumber());
        d.setConsultationFee(r.consultationFee());
        d.setBio(r.bio());
        d.setSlotMinutes(r.slotMinutes());
        d.setMaxPatientsPerSlot(r.maxPatientsPerSlot());
        d.setActive(r.active());
    }

    // ---------- receptionists / users ----------

    public List<UserView> receptionists() {
        return users.findByRoleOrderByFullName(Role.RECEPTIONIST).stream().map(mapper::user).toList();
    }

    @Transactional
    public UserView createReceptionist(StaffRequest r) {
        User u = newUser(r.fullName(), r.email(), r.phone(), r.password(), Role.RECEPTIONIST);
        Receptionist rc = new Receptionist();
        rc.setUser(u);
        receptionists.save(rc);
        audit.log("RECEPTIONIST_CREATED", "User", u.getId(), u.getEmail());
        return mapper.user(u);
    }

    @Transactional
    public UserView updateReceptionist(Long userId, StaffRequest r, User actor) {
        User u = users.findById(userId).orElseThrow(() -> ApiException.notFound("User not found"));
        if (u.getRole() != Role.RECEPTIONIST) throw ApiException.badRequest("Not a receptionist");
        updateUser(u, r.fullName(), r.email(), r.phone(), r.password());
        u.setEnabled(r.enabled());
        audit.log("RECEPTIONIST_UPDATED", "User", userId, null);
        return mapper.user(u);
    }

    public List<UserView> allUsers() {
        return users.findAll().stream().map(mapper::user).toList();
    }

    @Transactional
    public UserView setEnabled(Long userId, boolean enabled, User actor) {
        User u = users.findById(userId).orElseThrow(() -> ApiException.notFound("User not found"));
        if (u.getId().equals(actor.getId())) throw ApiException.badRequest("You cannot disable your own account");
        u.setEnabled(enabled);
        audit.log(enabled ? "USER_ENABLED" : "USER_DISABLED", "User", userId, u.getEmail());
        return mapper.user(u);
    }

    public List<com.munot.hospital.dto.PatientDtos.PatientView> patients() {
        return patients.findAll(PageRequest.of(0, 200, org.springframework.data.domain.Sort.by("createdAt").descending())).map(mapper::patient).getContent();
    }

    // ---------- departments & services ----------

    public List<DepartmentView> departments() {
        return departments.findAll().stream().map(d -> new DepartmentView(d.getId(), d.getName(), d.getDescription())).toList();
    }

    @Transactional
    public DepartmentView saveDepartment(Long id, DepartmentRequest r) {
        Department d = id == null ? new Department() : departments.findById(id).orElseThrow(() -> ApiException.notFound("Department not found"));
        if (id == null && departments.existsByNameIgnoreCase(r.name())) throw ApiException.conflict("Department already exists");
        d.setName(r.name().trim());
        d.setDescription(r.description());
        d = departments.save(d);
        audit.log("DEPARTMENT_SAVED", "Department", d.getId(), d.getName());
        return new DepartmentView(d.getId(), d.getName(), d.getDescription());
    }

    @Transactional
    public void deleteDepartment(Long id) {
        departments.deleteById(id);
        audit.log("DEPARTMENT_DELETED", "Department", id, null);
    }

    private ServiceView view(MedicalService s) {
        return new ServiceView(s.getId(), s.getName(), s.getDescription(), s.getDepartment() == null ? null : s.getDepartment().getId(),
                s.getDepartment() == null ? null : s.getDepartment().getName(), s.getPrice(), s.isActive());
    }

    public List<ServiceView> services(boolean activeOnly) {
        return (activeOnly ? services.findByActiveTrueOrderByName() : services.findAll()).stream().map(this::view).toList();
    }

    @Transactional
    public ServiceView saveService(Long id, ServiceRequest r) {
        MedicalService s = id == null ? new MedicalService() : services.findById(id).orElseThrow(() -> ApiException.notFound("Service not found"));
        s.setName(r.name().trim());
        s.setDescription(r.description());
        s.setDepartment(r.departmentId() == null ? null : departments.findById(r.departmentId()).orElseThrow(() -> ApiException.badRequest("Unknown department")));
        s.setPrice(r.price());
        s.setActive(r.active());
        s = services.save(s);
        audit.log("SERVICE_SAVED", "MedicalService", s.getId(), s.getName());
        return view(s);
    }

    @Transactional
    public void deleteService(Long id) {
        services.deleteById(id);
        audit.log("SERVICE_DELETED", "MedicalService", id, null);
    }

    // ---------- availability & leave ----------

    public List<AvailabilityDto> availability(Long doctorId) {
        return availability.findByDoctorId(doctorId).stream()
                .sorted(Comparator.comparing(DoctorAvailability::getDayOfWeek).thenComparing(DoctorAvailability::getStartTime))
                .map(a -> new AvailabilityDto(a.getDayOfWeek(), a.getStartTime(), a.getEndTime(), a.getBreakStart(), a.getBreakEnd(), a.isActive())).toList();
    }

    @Transactional
    public List<AvailabilityDto> replaceAvailability(Long doctorId, List<AvailabilityDto> list) {
        Doctor d = doctors.findById(doctorId).orElseThrow(() -> ApiException.notFound("Doctor not found"));
        for (AvailabilityDto a : list) {
            if (!a.endTime().isAfter(a.startTime())) throw ApiException.badRequest("End time must be after start time (" + a.dayOfWeek() + ")");
            if ((a.breakStart() == null) != (a.breakEnd() == null)) throw ApiException.badRequest("Provide both break start and end (" + a.dayOfWeek() + ")");
            if (a.breakStart() != null && (!a.breakEnd().isAfter(a.breakStart()) || a.breakStart().isBefore(a.startTime()) || a.breakEnd().isAfter(a.endTime())))
                throw ApiException.badRequest("Break must be inside working hours (" + a.dayOfWeek() + ")");
        }
        availability.deleteAll(availability.findByDoctorId(doctorId));
        availability.flush();
        for (AvailabilityDto a : list) {
            DoctorAvailability e = new DoctorAvailability();
            e.setDoctor(d);
            e.setDayOfWeek(a.dayOfWeek());
            e.setStartTime(a.startTime());
            e.setEndTime(a.endTime());
            e.setBreakStart(a.breakStart());
            e.setBreakEnd(a.breakEnd());
            e.setActive(a.active());
            availability.save(e);
        }
        audit.log("AVAILABILITY_UPDATED", "Doctor", doctorId, list.size() + " windows");
        return availability(doctorId);
    }

    public List<LeaveView> leaves(Long doctorId) {
        return leaves.findByDoctorIdOrderByLeaveDate(doctorId).stream().map(l -> new LeaveView(l.getId(), l.getLeaveDate(), l.getReason())).toList();
    }

    @Transactional
    public LeaveView addLeave(Long doctorId, LeaveRequest r) {
        Doctor d = doctors.findById(doctorId).orElseThrow(() -> ApiException.notFound("Doctor not found"));
        if (leaves.existsByDoctorIdAndLeaveDate(doctorId, r.date())) throw ApiException.conflict("Leave already added for that date");
        DoctorLeave l = new DoctorLeave();
        l.setDoctor(d);
        l.setLeaveDate(r.date());
        l.setReason(r.reason());
        l = leaves.save(l);
        audit.log("LEAVE_ADDED", "Doctor", doctorId, r.date().toString());
        return new LeaveView(l.getId(), l.getLeaveDate(), l.getReason());
    }

    @Transactional
    public void deleteLeave(Long leaveId) {
        leaves.deleteById(leaveId);
        audit.log("LEAVE_REMOVED", "DoctorLeave", leaveId, null);
    }

    // ---------- reports ----------

    public AdminSummary summary() {
        LocalDate today = LocalDate.now();
        LocalDate from = today.minusDays(29);
        List<Appointment> range = appointments.findByAppointmentDateBetween(from, today);
        Map<String, Long> status = range.stream().collect(Collectors.groupingBy(a -> a.getStatus().name(), TreeMap::new, Collectors.counting()));
        Map<LocalDate, Long> perDay = range.stream().collect(Collectors.groupingBy(Appointment::getAppointmentDate, Collectors.counting()));
        Map<LocalDate, BigDecimal> revenue = new HashMap<>();
        payments.findByPaidAtGreaterThanEqual(from.atStartOfDay()).forEach(p -> revenue.merge(p.getPaidAt().toLocalDate(), p.getAmount(), BigDecimal::add));
        List<DailyPoint> apptPoints = new ArrayList<>(), revPoints = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(today); d = d.plusDays(1)) {
            apptPoints.add(new DailyPoint(d.toString(), BigDecimal.valueOf(perDay.getOrDefault(d, 0L))));
            revPoints.add(new DailyPoint(d.toString(), revenue.getOrDefault(d, BigDecimal.ZERO)));
        }
        long todayCount = range.stream().filter(a -> a.getAppointmentDate().equals(today)).count();
        long completedToday = range.stream().filter(a -> a.getAppointmentDate().equals(today) && a.getStatus() == AppointmentStatus.COMPLETED).count();
        return new AdminSummary(patients.count(), doctors.findByActiveTrue().size(), todayCount, completedToday,
                revenue.getOrDefault(today, BigDecimal.ZERO), Optional.ofNullable(bills.totalPending()).orElse(BigDecimal.ZERO), status, apptPoints, revPoints);
    }

    public List<AuditView> auditLogs(int page) {
        return auditLogs.findAllByOrderByCreatedAtDesc(PageRequest.of(Math.max(page, 0), 100)).map(l ->
                new AuditView(l.getId(), l.getUsername(), l.getAction(), l.getEntityType(), l.getEntityId(), l.getDetails(), l.getIpAddress(), l.getCreatedAt())).getContent();
    }
}
