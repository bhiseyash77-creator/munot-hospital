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
@RequestMapping("/api/bills")
@RequiredArgsConstructor
public class BillController {
    private final BillingService billing;
    private final com.munot.hospital.pdf.PdfService pdf;

    @PostMapping
    @PreAuthorize("hasAnyRole('RECEPTIONIST','DOCTOR','ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public BillView create(@Valid @RequestBody BillRequest req) { return billing.create(req, CurrentUser.get()); }

    @GetMapping
    @PreAuthorize("hasAnyRole('RECEPTIONIST','DOCTOR','ADMIN')")
    public List<BillView> recent(@RequestParam(defaultValue = "false") boolean pendingOnly) { return billing.recent(pendingOnly); }

    @GetMapping("/my")
    @PreAuthorize("hasRole('PATIENT')")
    public List<BillView> mine() { return billing.mine(CurrentUser.get()); }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PATIENT','RECEPTIONIST','DOCTOR','ADMIN')")
    public BillView get(@PathVariable Long id) { return billing.get(id, CurrentUser.get()); }

    @PostMapping("/{id}/payments")
    @PreAuthorize("hasAnyRole('RECEPTIONIST','DOCTOR','ADMIN')")
    public BillView pay(@PathVariable Long id, @Valid @RequestBody PaymentRequest req) { return billing.addPayment(id, req, CurrentUser.get()); }

    @GetMapping("/{id}/pdf")
    @PreAuthorize("hasAnyRole('PATIENT','RECEPTIONIST','DOCTOR','ADMIN')")
    public ResponseEntity<byte[]> pdf(@PathVariable Long id) {
        byte[] data = pdf.invoicePdf(id, CurrentUser.get());
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"invoice-" + id + ".pdf\"").body(data);
    }
}
