package com.munot.hospital.repository;

import com.munot.hospital.entity.*;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.*;
import java.util.*;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {
    Optional<Doctor> findByUserId(Long userId);
    List<Doctor> findByActiveTrue();
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Doctor d where d.id = :id")
    Optional<Doctor> findByIdForUpdate(@Param("id") Long id);
}
