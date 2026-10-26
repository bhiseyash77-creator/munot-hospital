package com.munot.hospital.repository;

import com.munot.hospital.entity.*;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.*;
import java.util.*;

public interface BillRepository extends JpaRepository<Bill, Long> {
    Optional<Bill> findByAppointmentId(Long appointmentId);
    List<Bill> findByPatientIdOrderByCreatedAtDesc(Long patientId);
    List<Bill> findTop50ByOrderByCreatedAtDesc();
    List<Bill> findByCreatedAtBetween(java.time.LocalDateTime from, java.time.LocalDateTime to);
    List<Bill> findByStatusInOrderByCreatedAtDesc(Collection<PaymentStatus> statuses);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Bill b where b.id = :id")
    Optional<Bill> findByIdForUpdate(@Param("id") Long id);
    @Query("select sum(b.pendingAmount) from Bill b where b.status <> com.munot.hospital.entity.PaymentStatus.PAID")
    java.math.BigDecimal totalPending();
}
