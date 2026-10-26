package com.munot.hospital.repository;

import com.munot.hospital.entity.*;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.*;
import java.util.*;

public interface MedicalServiceRepository extends JpaRepository<MedicalService, Long> {
    List<MedicalService> findByActiveTrueOrderByName();
}
