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
@Table(name = "prescriptions")
public class Prescription extends BaseEntity {

    @Column(name = "prescription_code", nullable = false, length = 30) private String prescriptionCode;
    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "consultation_id") private Consultation consultation;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "patient_id") private Patient patient;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "doctor_id") private Doctor doctor;
    @Column(columnDefinition = "TEXT") private String notes;
    @OneToMany(mappedBy = "prescription", cascade = CascadeType.ALL, orphanRemoval = true) private List<PrescriptionItem> items = new ArrayList<>();
}
