package com.munot.hospital.controller;

import com.munot.hospital.dto.AppointmentDtos.*;
import com.munot.hospital.security.CurrentUser;
import com.munot.hospital.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/doctor")
@PreAuthorize("hasRole('DOCTOR')")
@RequiredArgsConstructor
public class DoctorScheduleController {
    private final DoctorBlockService blocks;
    private final AdminService admin;
    private final AccessControl access;

    @GetMapping("/availability")
    public List<AvailabilityDto> weeklyHours() { return admin.availability(access.doctorOf(CurrentUser.get()).getId()); }

    @GetMapping("/blocks")
    public List<BlockView> blocks() { return blocks.mine(CurrentUser.get()); }

    @PostMapping("/blocks")
    @ResponseStatus(HttpStatus.CREATED)
    public BlockView add(@Valid @RequestBody BlockRequest r) { return blocks.add(r, CurrentUser.get()); }

    @DeleteMapping("/blocks/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) { blocks.delete(id, CurrentUser.get()); }
}
