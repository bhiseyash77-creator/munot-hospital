package com.munot.hospital.repository;

import com.munot.hospital.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.*;
import java.util.*;

public interface SlotBlockRepository extends JpaRepository<SlotBlock, Long> {
    List<SlotBlock> findByDoctorIdAndBlockDate(Long doctorId, LocalDate date);
    List<SlotBlock> findByDoctorIdAndBlockDateGreaterThanEqualOrderByBlockDateAscStartTimeAsc(Long doctorId, LocalDate from);
}
