package com.munot.hospital.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.*;

@Getter
@Setter
@Entity
@Table(name = "slot_blocks")
public class SlotBlock extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "doctor_id") private Doctor doctor;
    @Column(name = "block_date", nullable = false) private LocalDate blockDate;
    @Column(name = "start_time") private LocalTime startTime;
    @Column(name = "end_time") private LocalTime endTime;
    @Column(length = 200) private String reason;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "created_by") private User createdBy;
}
