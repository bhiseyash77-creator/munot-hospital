package com.munot.hospital.repository;

import com.munot.hospital.entity.*;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.*;
import java.util.*;

public interface QueueEntryRepository extends JpaRepository<QueueEntry, Long> {
    Optional<QueueEntry> findByAppointmentId(Long appointmentId);
    List<QueueEntry> findByQueueDateOrderByTokenNumber(LocalDate date);
    List<QueueEntry> findByDoctorIdAndQueueDateOrderByTokenNumber(Long doctorId, LocalDate date);
    boolean existsByDoctorIdAndQueueDateAndStatus(Long doctorId, LocalDate date, QueueStatus status);
    @Query("select coalesce(max(q.tokenNumber), 0) from QueueEntry q where q.doctor.id = :d and q.queueDate = :date")
    int maxToken(@Param("d") Long doctorId, @Param("date") LocalDate date);
}
