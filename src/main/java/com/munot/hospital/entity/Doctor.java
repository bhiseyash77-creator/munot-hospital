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
@Table(name = "doctors")
public class Doctor extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id") private User user;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "department_id") private Department department;
    @Column(length = 150) private String specialization;
    @Column(length = 255) private String qualification;
    @Column(name = "registration_number", length = 80) private String registrationNumber;
    @Column(name = "consultation_fee", nullable = false, precision = 10, scale = 2) private BigDecimal consultationFee = BigDecimal.ZERO;
    @Column(columnDefinition = "TEXT") private String bio;
    @Column(name = "slot_minutes", nullable = false) private int slotMinutes = 15;
    @Column(name = "max_patients_per_slot", nullable = false) private int maxPatientsPerSlot = 1;
    @Column(nullable = false) private boolean active = true;
    @Column(name = "photo_stored_name", length = 100) private String photoStoredName;
    @Column(name = "photo_content_type", length = 50) private String photoContentType;
}
