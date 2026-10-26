package com.munot.hospital.repository;

import com.munot.hospital.entity.*;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.*;
import java.util.*;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByPaidAtGreaterThanEqual(LocalDateTime from);
    List<Payment> findByPaidAtBetween(LocalDateTime from, LocalDateTime to);
}
