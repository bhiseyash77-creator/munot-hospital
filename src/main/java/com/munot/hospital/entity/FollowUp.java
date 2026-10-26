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
@Table(name = "follow_ups")
public class FollowUp extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "consultation_id") private Consultation consultation;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "patient_id") private Patient patient;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "doctor_id") private Doctor doctor;
    @Column(name = "follow_up_date", nullable = false) private LocalDate followUpDate;
    @Column(name = "follow_up_time") private LocalTime followUpTime;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "appointment_id") private Appointment appointment;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private FollowUpStatus status;
    @Column(length = 300) private String notes;
}
