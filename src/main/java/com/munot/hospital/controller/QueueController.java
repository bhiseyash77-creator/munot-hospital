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
@RequestMapping("/api/queue")
@RequiredArgsConstructor
public class QueueController {
    private final AppointmentService appointments;
    private final ConsultationService consultations;

    @GetMapping
    @PreAuthorize("hasAnyRole('RECEPTIONIST','DOCTOR','ADMIN')")
    public List<QueueView> queue(@RequestParam(required = false) Long doctorId,
                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return appointments.queue(doctorId, date, CurrentUser.get());
    }

    /** Doctor calls the patient in: starts the consultation. */
    @PostMapping("/{appointmentId}/call")
    @PreAuthorize("hasRole('DOCTOR')")
    public ConsultationView call(@PathVariable Long appointmentId) { return consultations.start(appointmentId, CurrentUser.get()); }

    @PostMapping("/{appointmentId}/skip")
    @PreAuthorize("hasAnyRole('RECEPTIONIST','DOCTOR','ADMIN')")
    public QueueView skip(@PathVariable Long appointmentId) { return appointments.skip(appointmentId, CurrentUser.get()); }
}
