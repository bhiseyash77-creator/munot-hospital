//package com.munot.hospital.controller;
//
//import com.munot.hospital.dto.*;
//import com.munot.hospital.dto.AdminDtos.*;
//import com.munot.hospital.dto.AppointmentDtos.*;
//import com.munot.hospital.dto.AuthDtos.*;
//import com.munot.hospital.dto.BillingDtos.*;
//import com.munot.hospital.dto.ClinicalDtos.*;
//import com.munot.hospital.dto.PatientDtos.*;
//import com.munot.hospital.entity.*;
//import com.munot.hospital.security.CurrentUser;
//import com.munot.hospital.service.*;
//import jakarta.validation.Valid;
//import lombok.RequiredArgsConstructor;
//import org.springframework.format.annotation.DateTimeFormat;
//import org.springframework.http.*;
//import org.springframework.security.access.prepost.PreAuthorize;
//import org.springframework.web.bind.annotation.*;
//import java.time.*;
//import java.util.*;
//
//@RestController
//@RequestMapping("/api/patients")
//@RequiredArgsConstructor
//public class PatientController {
//    private final PatientService patients;
//    private final ConsultationService consultations;
//    private final DocumentService documents;
//    private final BillingService billing;
//
//    @PostMapping
//    @PreAuthorize("hasAnyRole('RECEPTIONIST','ADMIN')")
//    @ResponseStatus(HttpStatus.CREATED)
//    public PatientView create(@Valid @RequestBody PatientRequest req) { return patients.create(req); }
//
//    @PutMapping("/{id}")
//    @PreAuthorize("hasAnyRole('RECEPTIONIST','ADMIN')")
//    public PatientView update(@PathVariable Long id, @Valid @RequestBody PatientRequest req) { return patients.update(id, req); }
//
//    @GetMapping("/search")
//    @PreAuthorize("hasAnyRole('RECEPTIONIST','DOCTOR','ADMIN')")
//    public List<PatientView> search(@RequestParam String q) { return patients.search(q, CurrentUser.get()); }
//
//    @GetMapping("/recent")
//    @PreAuthorize("hasAnyRole('RECEPTIONIST','ADMIN')")
//    public List<PatientView> recent() { return patients.recent(); }
//
//    @GetMapping("/me")
//    @PreAuthorize("hasRole('PATIENT')")
//    public PatientView me() { return patients.me(CurrentUser.get()); }
//
//    @PutMapping("/me")
//    @PreAuthorize("hasRole('PATIENT')")
//    public PatientView updateMe(@Valid @RequestBody PatientRequest req) { return patients.updateMe(CurrentUser.get(), req); }
//
//    @GetMapping("/{id}")
//    @PreAuthorize("hasAnyRole('RECEPTIONIST','DOCTOR','ADMIN')")
//    public PatientView get(@PathVariable Long id) { return patients.get(id, CurrentUser.get()); }
//
//    /** Full clinical history: treating doctor only. */
//    @GetMapping("/{id}/history")
//    @PreAuthorize("hasRole('DOCTOR')")
//    public HistoryView history(@PathVariable Long id) { return consultations.history(id, CurrentUser.get()); }
//
//    @GetMapping("/{id}/documents")
//    @PreAuthorize("hasAnyRole('RECEPTIONIST','DOCTOR')")
//    public List<ReportView> documents(@PathVariable Long id) { return documents.forPatient(id, CurrentUser.get()); }
//
//    @GetMapping("/{id}/bills")
//    @PreAuthorize("hasAnyRole('RECEPTIONIST','ADMIN')")
//    public List<BillView> bills(@PathVariable Long id) { return billing.forPatient(id); }
//}
package com.munot.hospital.controller;

import com.munot.hospital.dto.*;
import com.munot.hospital.dto.AdminDtos.*;
import com.munot.hospital.dto.AppointmentDtos.*;
import com.munot.hospital.dto.AuthDtos.*;
import com.munot.hospital.dto.BillingDtos.*;
import com.munot.hospital.dto.ClinicalDtos.*;
import com.munot.hospital.dto.PatientDtos.*;
import com.munot.hospital.entity.*;
import com.munot.hospital.security.CurrentUser;
import com.munot.hospital.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.*;
import java.util.*;

@RestController
@RequestMapping("/api/patients")
@RequiredArgsConstructor
public class PatientController {
    private final PatientService patients;
    private final ConsultationService consultations;
    private final DocumentService documents;
    private final BillingService billing;

    @PostMapping
    @PreAuthorize("hasAnyRole('RECEPTIONIST','DOCTOR','ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public PatientView create(@Valid @RequestBody PatientRequest req) { return patients.create(req); }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('RECEPTIONIST','DOCTOR','ADMIN')")
    public PatientView update(@PathVariable Long id, @Valid @RequestBody PatientRequest req) { return patients.update(id, req); }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('RECEPTIONIST','DOCTOR','ADMIN')")
    public List<PatientView> search(@RequestParam String q) { return patients.search(q, CurrentUser.get()); }

    @GetMapping("/recent")
    @PreAuthorize("hasAnyRole('RECEPTIONIST','DOCTOR','ADMIN')")
    public List<PatientView> recent() { return patients.recent(); }

    @GetMapping("/me")
    @PreAuthorize("hasRole('PATIENT')")
    public PatientView me() { return patients.me(CurrentUser.get()); }

    @PutMapping("/me")
    @PreAuthorize("hasRole('PATIENT')")
    public PatientView updateMe(@Valid @RequestBody PatientRequest req) { return patients.updateMe(CurrentUser.get(), req); }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('RECEPTIONIST','DOCTOR','ADMIN')")
    public PatientView get(@PathVariable Long id) { return patients.get(id, CurrentUser.get()); }

    /** Full clinical history: treating doctor only. */
    @GetMapping("/{id}/history")
    @PreAuthorize("hasRole('DOCTOR')")
    public HistoryView history(@PathVariable Long id) { return consultations.history(id, CurrentUser.get()); }

    @GetMapping("/{id}/documents")
    @PreAuthorize("hasAnyRole('RECEPTIONIST','DOCTOR')")
    public List<ReportView> documents(@PathVariable Long id) { return documents.forPatient(id, CurrentUser.get()); }

    @GetMapping("/{id}/bills")
    @PreAuthorize("hasAnyRole('RECEPTIONIST','DOCTOR','ADMIN')")
    public List<BillView> bills(@PathVariable Long id) { return billing.forPatient(id); }
}
