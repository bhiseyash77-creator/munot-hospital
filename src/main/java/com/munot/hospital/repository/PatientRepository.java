package com.munot.hospital.repository;

import com.munot.hospital.entity.*;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.*;
import java.util.*;

public interface PatientRepository extends JpaRepository<Patient, Long> {
    Optional<Patient> findByUserId(Long userId);
    List<Patient> findByCreatedAtBetween(LocalDateTime from, LocalDateTime to);
    Optional<Patient> findByPatientCode(String code);
    Optional<Patient> findFirstByPhoneAndFullNameIgnoreCase(String phone, String fullName);
    @Query("select p from Patient p where lower(p.fullName) like lower(concat('%', :q, '%')) or p.phone like concat('%', :q, '%') or lower(p.patientCode) like lower(concat('%', :q, '%'))")
    Page<Patient> search(@Param("q") String q, Pageable pageable);
}
