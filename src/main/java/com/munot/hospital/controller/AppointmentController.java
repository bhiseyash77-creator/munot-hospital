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
//@RequestMapping("/api/appointments")
//@RequiredArgsConstructor
//public class AppointmentController {
//    private final AppointmentService appointments;
//
//    @PostMapping
//    @PreAuthorize("hasAnyRole('PATIENT','RECEPTIONIST','ADMIN')")
//    @ResponseStatus(HttpStatus.CREATED)
//    public AppointmentView book(@Valid @RequestBody BookRequest req) { return appointments.book(req, CurrentUser.get()); }
//
//    @GetMapping("/my")
//    @PreAuthorize("hasRole('PATIENT')")
//    public List<AppointmentView> mine() { return appointments.mine(CurrentUser.get()); }
//
//    @GetMapping
//    @PreAuthorize("hasAnyRole('RECEPTIONIST','DOCTOR','ADMIN')")
//    public List<AppointmentView> list(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
//                                      @RequestParam(required = false) Long doctorId,
//                                      @RequestParam(required = false) AppointmentStatus status) {
//        return appointments.list(date, doctorId, status, CurrentUser.get());
//    }
//
//    @GetMapping("/slots")
//    @PreAuthorize("hasAnyRole('RECEPTIONIST','ADMIN','PATIENT')")
//    public List<SlotView> slots(@RequestParam Long doctorId, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
//                                @RequestParam(defaultValue = "false") boolean walkIn) {
//        boolean staff = CurrentUser.get().getRole() != Role.PATIENT;
//        return appointments.slots(doctorId, date, walkIn && staff);
//    }
//
//    @GetMapping("/{id}")
//    public AppointmentView get(@PathVariable Long id) { return appointments.get(id, CurrentUser.get()); }
//
//    @PutMapping("/{id}/cancel")
//    @PreAuthorize("hasAnyRole('PATIENT','RECEPTIONIST','DOCTOR','ADMIN')")
//    public AppointmentView cancel(@PathVariable Long id, @Valid @RequestBody(required = false) CancelRequest req) {
//        return appointments.cancel(id, req == null ? null : req.reason(), CurrentUser.get());
//    }
//
//    @PutMapping("/{id}/reschedule")
//    @PreAuthorize("hasAnyRole('PATIENT','RECEPTIONIST','ADMIN')")
//    public AppointmentView reschedule(@PathVariable Long id, @Valid @RequestBody RescheduleRequest req) {
//        return appointments.reschedule(id, req, CurrentUser.get());
//    }
//
//    @PutMapping("/{id}/confirm")
//    @PreAuthorize("hasAnyRole('RECEPTIONIST','DOCTOR','ADMIN')")
//    public AppointmentView confirm(@PathVariable Long id) { return appointments.confirm(id, CurrentUser.get()); }
//
//    @GetMapping("/pending")
//    @PreAuthorize("hasAnyRole('RECEPTIONIST','DOCTOR','ADMIN')")
//    public List<AppointmentView> pending() { return appointments.pending(CurrentUser.get()); }
//
//    @PutMapping("/{id}/no-show")
//    @PreAuthorize("hasAnyRole('RECEPTIONIST','ADMIN')")
//    public AppointmentView noShow(@PathVariable Long id) { return appointments.markNoShow(id); }
//
//    @PostMapping("/{id}/check-in")
//    @PreAuthorize("hasAnyRole('RECEPTIONIST','ADMIN')")
//    public QueueView checkIn(@PathVariable Long id) { return appointments.checkIn(id); }
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
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
public class AppointmentController {
    private final AppointmentService appointments;

    @PostMapping
    @PreAuthorize("hasAnyRole('PATIENT','RECEPTIONIST','DOCTOR','ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public AppointmentView book(@Valid @RequestBody BookRequest req) { return appointments.book(req, CurrentUser.get()); }

    @GetMapping("/my")
    @PreAuthorize("hasRole('PATIENT')")
    public List<AppointmentView> mine() { return appointments.mine(CurrentUser.get()); }

    @GetMapping
    @PreAuthorize("hasAnyRole('RECEPTIONIST','DOCTOR','ADMIN')")
    public List<AppointmentView> list(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                      @RequestParam(required = false) Long doctorId,
                                      @RequestParam(required = false) AppointmentStatus status) {
        return appointments.list(date, doctorId, status, CurrentUser.get());
    }

    @GetMapping("/slots")
    @PreAuthorize("hasAnyRole('RECEPTIONIST','DOCTOR','ADMIN','PATIENT')")
    public List<SlotView> slots(@RequestParam Long doctorId, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                @RequestParam(defaultValue = "false") boolean walkIn) {
        boolean staff = CurrentUser.get().getRole() != Role.PATIENT;
        return appointments.slots(doctorId, date, walkIn && staff);
    }

    @GetMapping("/{id}")
    public AppointmentView get(@PathVariable Long id) { return appointments.get(id, CurrentUser.get()); }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('PATIENT','RECEPTIONIST','DOCTOR','ADMIN')")
    public AppointmentView cancel(@PathVariable Long id, @Valid @RequestBody(required = false) CancelRequest req) {
        return appointments.cancel(id, req == null ? null : req.reason(), CurrentUser.get());
    }

    @PutMapping("/{id}/reschedule")
    @PreAuthorize("hasAnyRole('PATIENT','RECEPTIONIST','DOCTOR','ADMIN')")
    public AppointmentView reschedule(@PathVariable Long id, @Valid @RequestBody RescheduleRequest req) {
        return appointments.reschedule(id, req, CurrentUser.get());
    }

    @PutMapping("/{id}/confirm")
    @PreAuthorize("hasAnyRole('RECEPTIONIST','DOCTOR','ADMIN')")
    public AppointmentView confirm(@PathVariable Long id) { return appointments.confirm(id, CurrentUser.get()); }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('RECEPTIONIST','DOCTOR','ADMIN')")
    public List<AppointmentView> pending() { return appointments.pending(CurrentUser.get()); }

    @PutMapping("/{id}/no-show")
    @PreAuthorize("hasAnyRole('RECEPTIONIST','DOCTOR','ADMIN')")
    public AppointmentView noShow(@PathVariable Long id) { return appointments.markNoShow(id); }

    @PostMapping("/{id}/check-in")
    @PreAuthorize("hasAnyRole('RECEPTIONIST','DOCTOR','ADMIN')")
    public QueueView checkIn(@PathVariable Long id) { return appointments.checkIn(id); }
}
