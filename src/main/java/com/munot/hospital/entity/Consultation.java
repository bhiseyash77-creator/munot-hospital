//package com.munot.hospital.entity;
//
//import jakarta.persistence.*;
//import lombok.Getter;
//import lombok.Setter;
//import java.math.BigDecimal;
//import java.time.*;
//import java.util.*;
//
//@Getter
//@Setter
//@Entity
//@Table(name = "consultations")
//public class Consultation extends BaseEntity {
//
//    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "appointment_id") private Appointment appointment;
//    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "patient_id") private Patient patient;
//    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "doctor_id") private Doctor doctor;
//    @Column(columnDefinition = "TEXT") private String symptoms;
//    @Column(name = "examination_notes", columnDefinition = "TEXT") private String examinationNotes;
//    @Column(name = "treatment_plan", columnDefinition = "TEXT") private String treatmentPlan;
//    @Column(name = "tests_advised", columnDefinition = "TEXT") private String testsAdvised;
//    @Column(columnDefinition = "TEXT") private String advice;
//    @Column(nullable = false, length = 2) private String language = "EN";
//    @Column(name = "advice_local", columnDefinition = "TEXT") private String adviceLocal;
//    @Column(name = "tests_advised_local", columnDefinition = "TEXT") private String testsAdvisedLocal;
//    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private ConsultationStatus status;
//    @Column(name = "started_at", nullable = false) private LocalDateTime startedAt;
//    @Column(name = "completed_at") private LocalDateTime completedAt;
//    @OneToMany(mappedBy = "consultation", cascade = CascadeType.ALL, orphanRemoval = true) private List<Diagnosis> diagnoses = new ArrayList<>();
//}
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
@Table(name = "consultations")
public class Consultation extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "appointment_id") private Appointment appointment;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "patient_id") private Patient patient;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "doctor_id") private Doctor doctor;
    @Column(columnDefinition = "TEXT") private String symptoms;
    @Column(name = "examination_notes", columnDefinition = "TEXT") private String examinationNotes;
    @Column(name = "treatment_plan", columnDefinition = "TEXT") private String treatmentPlan;
    @Column(name = "tests_advised", columnDefinition = "TEXT") private String testsAdvised;
    @Column(columnDefinition = "TEXT") private String advice;
    @Column(nullable = false, length = 2) private String language = "EN";
    @Column(name = "advice_local", columnDefinition = "TEXT") private String adviceLocal;
    @Column(name = "tests_advised_local", columnDefinition = "TEXT") private String testsAdvisedLocal;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private ConsultationStatus status;
    @Column(name = "started_at", nullable = false) private LocalDateTime startedAt;
    @Column(name = "completed_at") private LocalDateTime completedAt;
    @Column(name = "follow_up_days") private Integer followUpDays;
    @OneToMany(mappedBy = "consultation", cascade = CascadeType.ALL, orphanRemoval = true) private List<Diagnosis> diagnoses = new ArrayList<>();
}
