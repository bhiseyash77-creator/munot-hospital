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
@Table(name = "prescription_items")
public class PrescriptionItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "prescription_id") private Prescription prescription;
    @Column(name = "medicine_name", nullable = false, length = 200) private String medicineName;
    @Column(length = 300) private String composition;
    @Column(length = 100) private String dosage;
    @Column(length = 100) private String frequency;
    @Column(length = 100) private String duration;
    @Column(length = 300) private String instructions;
    @Column(name = "frequency_local", length = 300) private String frequencyLocal;
    @Column(name = "duration_local", length = 300) private String durationLocal;
    @Column(name = "instructions_local", length = 300) private String instructionsLocal;
}
