package com.munot.hospital.service;

import com.munot.hospital.common.ApiException;
import com.munot.hospital.dto.AppointmentDtos.*;
import com.munot.hospital.entity.*;
import com.munot.hospital.repository.*;
import com.munot.hospital.util.BlockRules;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;

/** Lets a doctor block any date or time range at any moment; blocked times disappear from booking. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DoctorBlockService {
    private final SlotBlockRepository blocks;
    private final AppointmentRepository appointments;
    private final AccessControl access;
    private final AuditService audit;

    private BlockView view(SlotBlock b) {
        int affected = (int) appointments.findByDoctorIdAndAppointmentDateOrderByStartTime(b.getDoctor().getId(), b.getBlockDate()).stream()
                .filter(a -> !AvailabilityService.INACTIVE.contains(a.getStatus()) && a.getStatus() != AppointmentStatus.COMPLETED)
                .filter(a -> BlockRules.overlaps(b.getStartTime(), b.getEndTime(), a.getStartTime(), a.getEndTime())).count();
        return new BlockView(b.getId(), b.getBlockDate(), b.getStartTime(), b.getEndTime(), b.getReason(), b.getStartTime() == null, affected);
    }

    public List<BlockView> mine(User actor) {
        Doctor d = access.doctorOf(actor);
        return blocks.findByDoctorIdAndBlockDateGreaterThanEqualOrderByBlockDateAscStartTimeAsc(d.getId(), LocalDate.now()).stream().map(this::view).toList();
    }

    @Transactional
    public BlockView add(BlockRequest r, User actor) {
        Doctor d = access.doctorOf(actor);
        if (r.date().isBefore(LocalDate.now())) throw ApiException.badRequest("Choose today or a future date.");
        if (!BlockRules.validRange(r.startTime(), r.endTime())) throw ApiException.badRequest("Give both start and end time, with the end after the start - or leave both empty to block the whole day.");
        boolean whole = r.startTime() == null;
        for (SlotBlock e : blocks.findByDoctorIdAndBlockDate(d.getId(), r.date())) {
            if (e.getStartTime() == null || whole || BlockRules.overlaps(e.getStartTime(), e.getEndTime(), r.startTime(), r.endTime()))
                throw ApiException.conflict("That time is already blocked.");
        }
        SlotBlock b = new SlotBlock();
        b.setDoctor(d);
        b.setBlockDate(r.date());
        b.setStartTime(r.startTime());
        b.setEndTime(r.endTime());
        b.setReason(r.reason() == null || r.reason().isBlank() ? null : r.reason().trim());
        b.setCreatedBy(actor);
        b = blocks.save(b);
        audit.log("SLOTS_BLOCKED", "Doctor", d.getId(), r.date() + (whole ? " whole day" : " " + r.startTime() + "-" + r.endTime()));
        return view(b);
    }

    @Transactional
    public void delete(Long id, User actor) {
        Doctor d = access.doctorOf(actor);
        SlotBlock b = blocks.findById(id).orElseThrow(() -> ApiException.notFound("Block not found"));
        if (!b.getDoctor().getId().equals(d.getId())) throw ApiException.forbidden("Access denied");
        blocks.delete(b);
        audit.log("SLOTS_UNBLOCKED", "Doctor", d.getId(), b.getBlockDate().toString());
    }
}
