package com.munot.hospital.repository;

import com.munot.hospital.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface MedicineRepository extends JpaRepository<Medicine, Long> {
    Optional<Medicine> findByNameIgnoreCase(String name);
    List<Medicine> findTop10ByNameContainingIgnoreCaseOrCompositionContainingIgnoreCaseOrderByUseCountDescNameAsc(String name, String composition);
    List<Medicine> findTop10ByOrderByUseCountDescNameAsc();
}
