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
@Table(name = "doctor_availability")
public class DoctorAvailability extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "doctor_id") private Doctor doctor;
    @Enumerated(EnumType.STRING) @Column(name = "day_of_week", nullable = false, length = 12) private DayOfWeek dayOfWeek;
    @Column(name = "start_time", nullable = false) private LocalTime startTime;
    @Column(name = "end_time", nullable = false) private LocalTime endTime;
    @Column(name = "break_start") private LocalTime breakStart;
    @Column(name = "break_end") private LocalTime breakEnd;
    @Column(nullable = false) private boolean active = true;
}
