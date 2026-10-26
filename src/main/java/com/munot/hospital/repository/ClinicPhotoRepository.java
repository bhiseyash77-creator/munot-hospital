package com.munot.hospital.repository;

import com.munot.hospital.entity.ClinicPhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ClinicPhotoRepository extends JpaRepository<ClinicPhoto, Long> {
    List<ClinicPhoto> findByActiveTrueOrderBySortOrderAscIdAsc();
    List<ClinicPhoto> findAllByOrderBySortOrderAscIdAsc();
}
