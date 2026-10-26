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
@Table(name = "medical_services")
public class MedicalService extends BaseEntity {

    @Column(nullable = false, length = 150) private String name;
    @Column(length = 600) private String description;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "department_id") private Department department;
    @Column(nullable = false, precision = 10, scale = 2) private BigDecimal price = BigDecimal.ZERO;
    @Column(nullable = false) private boolean active = true;
}
