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
@RequestMapping("/api/prescriptions")
@RequiredArgsConstructor
public class PrescriptionController {
    private final ConsultationService consultations;
    private final com.munot.hospital.pdf.PdfService pdf;

    @GetMapping("/my")
    @PreAuthorize("hasRole('PATIENT')")
    public List<PrescriptionView> mine() { return consultations.myPrescriptions(CurrentUser.get()); }

    @GetMapping("/{id}")
    public PrescriptionView get(@PathVariable Long id) { return consultations.prescription(id, CurrentUser.get()); }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable Long id) {
        byte[] data = pdf.prescriptionPdf(id, CurrentUser.get());
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"prescription-" + id + ".pdf\"").body(data);
    }
}
