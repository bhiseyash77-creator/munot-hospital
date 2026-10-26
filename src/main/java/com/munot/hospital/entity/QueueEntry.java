package com.munot.hospital.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Getter
@Setter
@Entity
@Table(name = "queue_entries")
public class QueueEntry extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "appointment_id") private Appointment appointment;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "doctor_id") private Doctor doctor;
    @Column(name = "queue_date", nullable = false) private LocalDate queueDate;
    @Column(name = "token_number", nullable = false) private int tokenNumber;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private QueueStatus status;
    @Column(name = "checked_in_at", nullable = false) private LocalDateTime checkedInAt;
    @Column(name = "called_at") private LocalDateTime calledAt;
    @Column(name = "completed_at") private LocalDateTime completedAt;
}
