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
@Table(name = "medical_reports")
public class MedicalReport extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "patient_id") private Patient patient;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "appointment_id") private Appointment appointment;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "document_id") private PatientDocument document;
    @Enumerated(EnumType.STRING) @Column(name = "report_type", nullable = false, length = 20) private ReportType reportType;
    @Column(nullable = false, length = 200) private String title;
    @Column(length = 500) private String notes;
    @Column(name = "report_date", nullable = false) private LocalDate reportDate;
}
