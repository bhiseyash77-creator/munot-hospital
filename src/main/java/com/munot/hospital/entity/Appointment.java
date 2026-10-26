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
@Table(name = "appointments")
public class Appointment extends BaseEntity {

    @Column(name = "appointment_code", nullable = false, length = 40) private String appointmentCode;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "patient_id") private Patient patient;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "doctor_id") private Doctor doctor;
    @Column(name = "appointment_date", nullable = false) private LocalDate appointmentDate;
    @Column(name = "start_time", nullable = false) private LocalTime startTime;
    @Column(name = "end_time", nullable = false) private LocalTime endTime;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private AppointmentStatus status;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private AppointmentType type;
    @Column(length = 500) private String reason;
    @Column(name = "cancel_reason", length = 300) private String cancelReason;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "created_by") private User createdBy;
}
