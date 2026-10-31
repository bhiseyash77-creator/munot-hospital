//package com.munot.hospital.service;
//
//import com.munot.hospital.common.ApiException;
//import com.munot.hospital.entity.*;
//import com.munot.hospital.repository.*;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Component;
//
///** Central ownership / relationship checks. Every patient-scoped read goes through here. */
//@Component
//@RequiredArgsConstructor
//public class AccessControl {
//    private final PatientRepository patients;
//    private final DoctorRepository doctors;
//    private final AppointmentRepository appointments;
//
//    public Patient patientOf(User u) {
//        return patients.findByUserId(u.getId()).orElseThrow(() -> ApiException.forbidden("No patient profile linked to this account"));
//    }
//
//    public Doctor doctorOf(User u) {
//        return doctors.findByUserId(u.getId()).orElseThrow(() -> ApiException.forbidden("No doctor profile linked to this account"));
//    }
//
//    public boolean doctorTreats(User u, Long patientId) {
//        return doctors.findByUserId(u.getId()).map(d -> appointments.existsByDoctorIdAndPatientId(d.getId(), patientId)).orElse(false);
//    }
//
//    /** Patient record access: patient=self, doctor=treating, reception/admin=any. */
//    public Patient requirePatient(User u, Long patientId) {
//        Patient p = patients.findById(patientId).orElseThrow(() -> ApiException.notFound("Patient not found"));
//        switch (u.getRole()) {
//            case PATIENT -> { if (p.getUser() == null || !p.getUser().getId().equals(u.getId())) throw ApiException.forbidden("Access denied"); }
//            case DOCTOR -> { if (!doctorTreats(u, patientId)) throw ApiException.forbidden("This patient has no appointment with you"); }
//            default -> { }
//        }
//        return p;
//    }
//
//    /** Clinical data (consultations/diagnoses): patient=self, treating doctor only. */
//    public void requireClinical(User u, Long patientId) {
//        Patient p = patients.findById(patientId).orElseThrow(() -> ApiException.notFound("Patient not found"));
//        boolean ok = switch (u.getRole()) {
//            case PATIENT -> p.getUser() != null && p.getUser().getId().equals(u.getId());
//            case DOCTOR -> doctorTreats(u, patientId);
//            default -> false;
//        };
//        if (!ok) throw ApiException.forbidden("Access denied");
//    }
//
//    public Appointment requireAppointment(User u, Long appointmentId) {
//        Appointment a = appointments.findById(appointmentId).orElseThrow(() -> ApiException.notFound("Appointment not found"));
//        switch (u.getRole()) {
//            case PATIENT -> { if (a.getPatient().getUser() == null || !a.getPatient().getUser().getId().equals(u.getId())) throw ApiException.forbidden("Access denied"); }
//            case DOCTOR -> { if (!a.getDoctor().getUser().getId().equals(u.getId())) throw ApiException.forbidden("Access denied"); }
//            default -> { }
//        }
//        return a;
//    }
//}
package com.munot.hospital.service;

import com.munot.hospital.common.ApiException;
import com.munot.hospital.entity.*;
import com.munot.hospital.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Central ownership / relationship checks. Every patient-scoped read goes through here. */
@Component
@RequiredArgsConstructor
public class AccessControl {
    private final PatientRepository patients;
    private final DoctorRepository doctors;
    private final AppointmentRepository appointments;

    public Patient patientOf(User u) {
        return patients.findByUserId(u.getId()).orElseThrow(() -> ApiException.forbidden("No patient profile linked to this account"));
    }

    public Doctor doctorOf(User u) {
        return doctors.findByUserId(u.getId()).orElseThrow(() -> ApiException.forbidden("No doctor profile linked to this account"));
    }

    public boolean doctorTreats(User u, Long patientId) {
        return doctors.findByUserId(u.getId()).map(d -> appointments.existsByDoctorIdAndPatientId(d.getId(), patientId)).orElse(false);
    }

    /** Patient record access: patient=self, doctor=treating, reception/admin=any. */
    public Patient requirePatient(User u, Long patientId) {
        Patient p = patients.findById(patientId).orElseThrow(() -> ApiException.notFound("Patient not found"));
        switch (u.getRole()) {
            case PATIENT -> { if (p.getUser() == null || !p.getUser().getId().equals(u.getId())) throw ApiException.forbidden("Access denied"); }
            case DOCTOR -> { if (!doctorTreats(u, patientId)) throw ApiException.forbidden("This patient has no appointment with you"); }
            default -> { }
        }
        return p;
    }

    /**
     * Non-clinical patient record (demographics, uploaded documents): doctors get the same reception-level access as front-desk staff,
     * so a doctor can register patients, book and check people in. Clinical data still goes through requirePatient / requireClinical.
     */
    public Patient requirePatientRecord(User u, Long patientId) {
        if (u.getRole() == Role.DOCTOR) return patients.findById(patientId).orElseThrow(() -> ApiException.notFound("Patient not found"));
        return requirePatient(u, patientId);
    }

    /** Clinical data (consultations/diagnoses): patient=self, treating doctor only. */
    public void requireClinical(User u, Long patientId) {
        Patient p = patients.findById(patientId).orElseThrow(() -> ApiException.notFound("Patient not found"));
        boolean ok = switch (u.getRole()) {
            case PATIENT -> p.getUser() != null && p.getUser().getId().equals(u.getId());
            case DOCTOR -> doctorTreats(u, patientId);
            default -> false;
        };
        if (!ok) throw ApiException.forbidden("Access denied");
    }

    public Appointment requireAppointment(User u, Long appointmentId) {
        Appointment a = appointments.findById(appointmentId).orElseThrow(() -> ApiException.notFound("Appointment not found"));
        switch (u.getRole()) {
            case PATIENT -> { if (a.getPatient().getUser() == null || !a.getPatient().getUser().getId().equals(u.getId())) throw ApiException.forbidden("Access denied"); }
            case DOCTOR -> { if (!a.getDoctor().getUser().getId().equals(u.getId())) throw ApiException.forbidden("Access denied"); }
            default -> { }
        }
        return a;
    }
}
