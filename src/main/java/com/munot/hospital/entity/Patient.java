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
@Table(name = "patients")
public class Patient extends BaseEntity {

    @Column(name = "patient_code", nullable = false, length = 20) private String patientCode;
    @OneToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id") private User user;
    @Column(name = "full_name", nullable = false, length = 120) private String fullName;
    @Column(nullable = false, length = 20) private String phone;
    @Column(length = 150) private String email;
    @Column(name = "date_of_birth") private LocalDate dateOfBirth;
    @Column(length = 20) private String gender;
    @Column(length = 300) private String address;
    @Column(name = "blood_group", length = 10) private String bloodGroup;
    @Column(name = "emergency_contact_name", length = 120) private String emergencyContactName;
    @Column(name = "emergency_contact_phone", length = 20) private String emergencyContactPhone;
}
