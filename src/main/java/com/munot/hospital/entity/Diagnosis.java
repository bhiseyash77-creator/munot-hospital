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
@Table(name = "diagnoses")
public class Diagnosis extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "consultation_id") private Consultation consultation;
    @Column(nullable = false, length = 200) private String name;
    @Column(name = "icd_code", length = 20) private String icdCode;
    @Column(length = 500) private String notes;
}
