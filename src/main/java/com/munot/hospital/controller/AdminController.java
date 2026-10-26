package com.munot.hospital.controller;

import com.munot.hospital.dto.*;
import com.munot.hospital.dto.ContentDtos.*;
import com.munot.hospital.dto.ClinicDtos.*;
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
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {
    private final AdminService admin;
    private final SettingsService settings;
    private final ContentService content;
    private final DoctorPhotoService photos;
    private final ClinicPhotoService clinicPhotos;

    @GetMapping("/summary") public AdminSummary summary() { return admin.summary(); }

    @GetMapping("/doctors") public List<DoctorView> doctors() { return admin.doctors(false); }
    @PostMapping("/doctors") @ResponseStatus(HttpStatus.CREATED) public DoctorView createDoctor(@Valid @RequestBody DoctorRequest r) { return admin.createDoctor(r); }
    @PutMapping("/doctors/{id}") public DoctorView updateDoctor(@PathVariable Long id, @Valid @RequestBody DoctorRequest r) { return admin.updateDoctor(id, r); }

    @GetMapping("/doctors/{id}/availability") public List<AvailabilityDto> availability(@PathVariable Long id) { return admin.availability(id); }
    @PutMapping("/doctors/{id}/availability") public List<AvailabilityDto> setAvailability(@PathVariable Long id, @Valid @RequestBody List<AvailabilityDto> list) { return admin.replaceAvailability(id, list); }
    @GetMapping("/doctors/{id}/leaves") public List<LeaveView> leaves(@PathVariable Long id) { return admin.leaves(id); }
    @PostMapping("/doctors/{id}/leaves") @ResponseStatus(HttpStatus.CREATED) public LeaveView addLeave(@PathVariable Long id, @Valid @RequestBody LeaveRequest r) { return admin.addLeave(id, r); }
    @DeleteMapping("/leaves/{leaveId}") @ResponseStatus(HttpStatus.NO_CONTENT) public void deleteLeave(@PathVariable Long leaveId) { admin.deleteLeave(leaveId); }

    @GetMapping("/receptionists") public List<UserView> receptionists() { return admin.receptionists(); }
    @PostMapping("/receptionists") @ResponseStatus(HttpStatus.CREATED) public UserView createReceptionist(@Valid @RequestBody StaffRequest r) { return admin.createReceptionist(r); }
    @PutMapping("/receptionists/{userId}") public UserView updateReceptionist(@PathVariable Long userId, @Valid @RequestBody StaffRequest r) { return admin.updateReceptionist(userId, r, CurrentUser.get()); }

    @GetMapping("/users") public List<UserView> users() { return admin.allUsers(); }
    @PutMapping("/users/{id}/enabled") public UserView setEnabled(@PathVariable Long id, @RequestParam boolean value) { return admin.setEnabled(id, value, CurrentUser.get()); }
    @GetMapping("/patients") public List<PatientView> patients() { return admin.patients(); }

    @GetMapping("/departments") public List<DepartmentView> departments() { return admin.departments(); }
    @PostMapping("/departments") @ResponseStatus(HttpStatus.CREATED) public DepartmentView createDepartment(@Valid @RequestBody DepartmentRequest r) { return admin.saveDepartment(null, r); }
    @PutMapping("/departments/{id}") public DepartmentView updateDepartment(@PathVariable Long id, @Valid @RequestBody DepartmentRequest r) { return admin.saveDepartment(id, r); }
    @DeleteMapping("/departments/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void deleteDepartment(@PathVariable Long id) { admin.deleteDepartment(id); }

    @GetMapping("/services") public List<ServiceView> services() { return admin.services(false); }
    @PostMapping("/services") @ResponseStatus(HttpStatus.CREATED) public ServiceView createService(@Valid @RequestBody ServiceRequest r) { return admin.saveService(null, r); }
    @PutMapping("/services/{id}") public ServiceView updateService(@PathVariable Long id, @Valid @RequestBody ServiceRequest r) { return admin.saveService(id, r); }
    @DeleteMapping("/services/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void deleteService(@PathVariable Long id) { admin.deleteService(id); }

    @GetMapping("/settings") public Map<String, String> settings() { return settings.all(); }
    @PutMapping("/settings") public Map<String, String> updateSettings(@RequestBody Map<String, String> values) { return settings.update(values); }

    @GetMapping("/audit-logs") public List<AuditView> audit(@RequestParam(defaultValue = "0") int page) { return admin.auditLogs(page); }

    @GetMapping("/content") public List<ContentView> contentList(@RequestParam ContentType type) { return content.adminList(type); }
    @PostMapping("/content") @ResponseStatus(HttpStatus.CREATED) public ContentView createContent(@Valid @RequestBody ContentRequest r) { return content.save(null, r); }
    @PutMapping("/content/{id}") public ContentView updateContent(@PathVariable Long id, @Valid @RequestBody ContentRequest r) { return content.save(id, r); }
    @DeleteMapping("/content/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void deleteContent(@PathVariable Long id) { content.delete(id); }

    @PostMapping(value = "/doctors/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void uploadPhoto(@PathVariable Long id, @RequestParam("file") org.springframework.web.multipart.MultipartFile file) throws java.io.IOException { photos.upload(id, file); }

    @DeleteMapping("/doctors/{id}/photo") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removePhoto(@PathVariable Long id) throws java.io.IOException { photos.remove(id); }

    @GetMapping("/clinic-photos") public List<ClinicPhotoView> clinicPhotoList() { return clinicPhotos.adminList(); }

    @PostMapping(value = "/clinic-photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ClinicPhotoView addClinicPhoto(@RequestParam("file") org.springframework.web.multipart.MultipartFile file,
                                          @RequestParam(required = false) String caption, @RequestParam(defaultValue = "0") int sortOrder) throws java.io.IOException {
        return clinicPhotos.upload(file, caption, sortOrder);
    }

    @PutMapping("/clinic-photos/{id}") public ClinicPhotoView updateClinicPhoto(@PathVariable Long id, @Valid @RequestBody ClinicPhotoUpdate u) { return clinicPhotos.update(id, u); }

    @DeleteMapping("/clinic-photos/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteClinicPhoto(@PathVariable Long id) throws java.io.IOException { clinicPhotos.delete(id); }

    /** Admin preview, including photos hidden from the website. */
    @GetMapping("/clinic-photos/{id}/image")
    public ResponseEntity<org.springframework.core.io.Resource> clinicPhotoPreview(@PathVariable Long id) {
        DocumentService.Download d = clinicPhotos.load(id, false);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(d.contentType())).header("X-Content-Type-Options", "nosniff").body(d.resource());
    }
}
