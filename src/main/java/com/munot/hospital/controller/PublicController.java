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
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicController {
    private final AdminService admin;
    private final SettingsService settings;
    private final AppointmentService appointments;
    private final ContentService content;
    private final DoctorPhotoService photos;
    private final ClinicPhotoService clinicPhotos;

    @GetMapping("/settings")
    public Map<String, String> settings() { return settings.publicSettings(); }

    @GetMapping("/doctors")
    public List<DoctorView> doctors() {
        // public view hides contact details of the doctor's login account
        return admin.doctors(true).stream().map(d -> new DoctorView(d.id(), null, d.fullName(), null, null, d.departmentId(), d.departmentName(),
                d.specialization(), d.qualification(), d.registrationNumber(), d.consultationFee(), d.bio(), d.slotMinutes(),
                d.maxPatientsPerSlot(), d.active(), d.hasPhoto(), d.photoVersion())).toList();
    }

    @GetMapping("/services")
    public List<ServiceView> services() { return admin.services(true); }

    @GetMapping("/departments")
    public List<DepartmentView> departments() { return admin.departments(); }

    @GetMapping("/doctors/{id}/slots")
    public List<SlotView> slots(@PathVariable Long id, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return appointments.slots(id, date, false);
    }

    @GetMapping("/content")
    public List<ContentView> content(@RequestParam ContentType type) { return content.publicList(type); }

    @GetMapping("/doctors/{id}/photo")
    public ResponseEntity<org.springframework.core.io.Resource> photo(@PathVariable Long id) {
        DocumentService.Download d = photos.load(id);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(d.contentType()))
                .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePublic())
                .header("X-Content-Type-Options", "nosniff").body(d.resource());
    }

    @GetMapping("/clinic-photos")
    public List<ClinicPhotoView> clinicPhotos() { return clinicPhotos.publicList(); }

    @GetMapping("/clinic-photos/{id}/image")
    public ResponseEntity<org.springframework.core.io.Resource> clinicPhotoImage(@PathVariable Long id) {
        DocumentService.Download d = clinicPhotos.load(id, true);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(d.contentType()))
                .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePublic())
                .header("X-Content-Type-Options", "nosniff").body(d.resource());
    }
}
