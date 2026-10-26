package com.munot.hospital.controller;

import com.munot.hospital.dto.ClinicalDtos.*;
import com.munot.hospital.security.CurrentUser;
import com.munot.hospital.service.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/visits")
public class VisitController {
    private final ConsultationService consultations;

    public VisitController(ConsultationService consultations) { this.consultations = consultations; }

    /** Patient portal: my completed visits (diagnosis, plan, advice, medicines, follow-up). */
    @GetMapping("/my")
    @PreAuthorize("hasRole('PATIENT')")
    public List<ConsultationView> mine() { return consultations.myVisits(CurrentUser.get()); }
}
