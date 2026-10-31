//package com.munot.hospital.service;
//
//import com.munot.hospital.common.ApiException;
//import com.munot.hospital.dto.PatientDtos.*;
//import com.munot.hospital.entity.*;
//import com.munot.hospital.repository.*;
//import com.munot.hospital.util.Codes;
//import com.munot.hospital.util.Phones;
//import lombok.RequiredArgsConstructor;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//import java.util.*;
//
//@Service
//@RequiredArgsConstructor
//@Transactional(readOnly = true)
//public class PatientService {
//    private final PatientRepository patients;
//    private final AppointmentRepository appointments;
//    private final AccessControl access;
//    private final ViewMapper mapper;
//    private final AuditService audit;
//
//    @Transactional
//    public PatientView create(PatientRequest req) {
//        String phone = Phones.normalize(req.phone());
//        patients.findFirstByPhoneAndFullNameIgnoreCase(phone, req.fullName().trim()).ifPresent(p -> {
//            throw ApiException.conflict("A patient with this name and phone already exists (" + p.getPatientCode() + ").");
//        });
//        Patient p = new Patient();
//        p.setPatientCode("TMP-" + UUID.randomUUID().toString().substring(0, 12));
//        apply(p, req, phone);
//        p = patients.saveAndFlush(p);
//        p.setPatientCode(Codes.of("PAT", p.getId()));
//        audit.log("PATIENT_REGISTERED", "Patient", p.getId(), p.getPatientCode());
//        return mapper.patient(p);
//    }
//
//    @Transactional
//    public PatientView update(Long id, PatientRequest req) {
//        Patient p = patients.findById(id).orElseThrow(() -> ApiException.notFound("Patient not found"));
//        String phone = Phones.normalize(req.phone());
//        patients.findFirstByPhoneAndFullNameIgnoreCase(phone, req.fullName().trim()).ifPresent(o -> {
//            if (!o.getId().equals(id)) throw ApiException.conflict("Another patient with this name and phone already exists (" + o.getPatientCode() + ").");
//        });
//        apply(p, req, phone);
//        audit.log("PATIENT_UPDATED", "Patient", id, null);
//        return mapper.patient(p);
//    }
//
//    private void apply(Patient p, PatientRequest r, String phone) {
//        p.setFullName(r.fullName().trim());
//        p.setPhone(phone);
//        p.setEmail(r.email() == null || r.email().isBlank() ? null : r.email().trim());
//        p.setDateOfBirth(r.dateOfBirth());
//        p.setGender(r.gender());
//        p.setAddress(r.address());
//        p.setBloodGroup(r.bloodGroup());
//        p.setEmergencyContactName(r.emergencyContactName());
//        p.setEmergencyContactPhone(r.emergencyContactPhone() == null ? null : Phones.normalize(r.emergencyContactPhone()));
//    }
//
//    /** Search by name / phone / patient code / appointment code. Doctors only see their own patients. */
//    public List<PatientView> search(String q, User actor) {
//        String term = q == null ? "" : q.trim();
//        if (term.length() < 2) return List.of();
//        Set<Patient> found = new LinkedHashSet<>();
//        if (term.toUpperCase().startsWith("APT-")) appointments.findByAppointmentCode(term.toUpperCase()).ifPresent(a -> found.add(a.getPatient()));
//        String digits = Phones.normalize(term);
//        found.addAll(patients.search(digits != null && digits.length() >= 4 && term.replaceAll("[\\d+\\s-]", "").isEmpty() ? digits : term,
//                PageRequest.of(0, 25)).getContent());
//        List<Patient> list = new ArrayList<>(found);
//        if (actor.getRole() == Role.DOCTOR) list = list.stream().filter(p -> access.doctorTreats(actor, p.getId())).toList();
//        return list.stream().map(mapper::patient).toList();
//    }
//
//    public PatientView get(Long id, User actor) {
//        return mapper.patient(access.requirePatient(actor, id));
//    }
//
//    public PatientView me(User actor) {
//        return mapper.patient(access.patientOf(actor));
//    }
//
//    @Transactional
//    public PatientView updateMe(User actor, PatientRequest req) {
//        Patient p = access.patientOf(actor);
//        return update(p.getId(), req);
//    }
//
//    public List<PatientView> recent() {
//        return patients.findAll(PageRequest.of(0, 50, org.springframework.data.domain.Sort.by("createdAt").descending())).map(mapper::patient).getContent();
//    }
//}
package com.munot.hospital.service;

import com.munot.hospital.common.ApiException;
import com.munot.hospital.dto.PatientDtos.*;
import com.munot.hospital.entity.*;
import com.munot.hospital.repository.*;
import com.munot.hospital.util.Codes;
import com.munot.hospital.util.Phones;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PatientService {
    private final PatientRepository patients;
    private final AppointmentRepository appointments;
    private final AccessControl access;
    private final ViewMapper mapper;
    private final AuditService audit;

    @Transactional
    public PatientView create(PatientRequest req) {
        String phone = Phones.normalize(req.phone());
        patients.findFirstByPhoneAndFullNameIgnoreCase(phone, req.fullName().trim()).ifPresent(p -> {
            throw ApiException.conflict("A patient with this name and phone already exists (" + p.getPatientCode() + ").");
        });
        Patient p = new Patient();
        p.setPatientCode("TMP-" + UUID.randomUUID().toString().substring(0, 12));
        apply(p, req, phone);
        p = patients.saveAndFlush(p);
        p.setPatientCode(Codes.of("PAT", p.getId()));
        audit.log("PATIENT_REGISTERED", "Patient", p.getId(), p.getPatientCode());
        return mapper.patient(p);
    }

    @Transactional
    public PatientView update(Long id, PatientRequest req) {
        Patient p = patients.findById(id).orElseThrow(() -> ApiException.notFound("Patient not found"));
        String phone = Phones.normalize(req.phone());
        patients.findFirstByPhoneAndFullNameIgnoreCase(phone, req.fullName().trim()).ifPresent(o -> {
            if (!o.getId().equals(id)) throw ApiException.conflict("Another patient with this name and phone already exists (" + o.getPatientCode() + ").");
        });
        apply(p, req, phone);
        audit.log("PATIENT_UPDATED", "Patient", id, null);
        return mapper.patient(p);
    }

    private void apply(Patient p, PatientRequest r, String phone) {
        p.setFullName(r.fullName().trim());
        p.setPhone(phone);
        p.setEmail(r.email() == null || r.email().isBlank() ? null : r.email().trim());
        p.setDateOfBirth(r.dateOfBirth());
        p.setGender(r.gender());
        p.setAddress(r.address());
        p.setBloodGroup(r.bloodGroup());
        p.setEmergencyContactName(r.emergencyContactName());
        p.setEmergencyContactPhone(r.emergencyContactPhone() == null ? null : Phones.normalize(r.emergencyContactPhone()));
    }

    /** Search by name / phone / patient code / appointment code. Doctors only see their own patients. */
    public List<PatientView> search(String q, User actor) {
        String term = q == null ? "" : q.trim();
        if (term.length() < 2) return List.of();
        Set<Patient> found = new LinkedHashSet<>();
        if (term.toUpperCase().startsWith("APT-")) appointments.findByAppointmentCode(term.toUpperCase()).ifPresent(a -> found.add(a.getPatient()));
        String digits = Phones.normalize(term);
        found.addAll(patients.search(digits != null && digits.length() >= 4 && term.replaceAll("[\\d+\\s-]", "").isEmpty() ? digits : term,
                PageRequest.of(0, 25)).getContent());
        List<Patient> list = new ArrayList<>(found);
        return list.stream().map(mapper::patient).toList();
    }

    public PatientView get(Long id, User actor) {
        return mapper.patient(access.requirePatientRecord(actor, id));
    }

    public PatientView me(User actor) {
        return mapper.patient(access.patientOf(actor));
    }

    @Transactional
    public PatientView updateMe(User actor, PatientRequest req) {
        Patient p = access.patientOf(actor);
        return update(p.getId(), req);
    }

    public List<PatientView> recent() {
        return patients.findAll(PageRequest.of(0, 50, org.springframework.data.domain.Sort.by("createdAt").descending())).map(mapper::patient).getContent();
    }
}
