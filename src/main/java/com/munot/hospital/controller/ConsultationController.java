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
@RequestMapping("/api/consultations")
@PreAuthorize("hasRole('DOCTOR')")
@RequiredArgsConstructor
public class ConsultationController {
    private final ConsultationService consultations;

    @GetMapping("/{id}")
    public ConsultationView get(@PathVariable Long id) { return consultations.get(id, CurrentUser.get()); }

    @PutMapping("/{id}")
    public ConsultationView save(@PathVariable Long id, @Valid @RequestBody ConsultationSaveRequest req) { return consultations.save(id, req, CurrentUser.get()); }

    @PostMapping("/{id}/complete")
    public CompleteResponse complete(@PathVariable Long id, @Valid @RequestBody CompleteRequest req) { return consultations.complete(id, req, CurrentUser.get()); }
}
