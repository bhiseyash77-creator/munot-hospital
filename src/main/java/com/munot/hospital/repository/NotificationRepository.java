package com.munot.hospital.repository;

import com.munot.hospital.entity.*;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.*;
import java.util.*;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    boolean existsByDedupeKey(String key);
    List<Notification> findByAppointmentIdAndStatus(Long appointmentId, NotificationStatus status);
    List<Notification> findTop100ByRecipientUserIdAndChannelAndStatusOrderByScheduledAtDesc(Long userId, Channel channel, NotificationStatus status);
    long countByRecipientUserIdAndChannelAndStatusAndReadFlagFalse(Long userId, Channel channel, NotificationStatus status);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    @Query("select n from Notification n where n.status = :status and n.scheduledAt <= :now order by n.scheduledAt")
    List<Notification> findDue(@Param("status") NotificationStatus status, @Param("now") LocalDateTime now, Pageable pageable);
}
