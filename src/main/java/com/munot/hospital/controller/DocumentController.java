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
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {
    private final DocumentService documents;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('RECEPTIONIST','DOCTOR')")
    @ResponseStatus(HttpStatus.CREATED)
    public ReportView upload(@RequestParam Long patientId, @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
                             @RequestParam(defaultValue = "OTHER") ReportType reportType, @RequestParam(required = false) String title,
                             @RequestParam(required = false) String notes,
                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate reportDate,
                             @RequestParam(required = false) Long appointmentId) throws java.io.IOException {
        return documents.upload(patientId, file, reportType, title, notes, reportDate, appointmentId, CurrentUser.get());
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('PATIENT')")
    public List<ReportView> mine() { return documents.mine(CurrentUser.get()); }

    @GetMapping("/{id}/download")
    public ResponseEntity<org.springframework.core.io.Resource> download(@PathVariable Long id) {
        DocumentService.Download d = documents.download(id, CurrentUser.get());
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(d.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(d.fileName()).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(d.resource());
    }
}
