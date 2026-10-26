package com.munot.hospital.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "medical_documents")
public class MedicalDocument extends BaseEntity {
    @Column(name = "doc_code", nullable = false, length = 20) private String docCode;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "patient_id") private Patient patient;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "doctor_id") private Doctor doctor;
    @Enumerated(EnumType.STRING) @Column(name = "doc_type", nullable = false, length = 30) private MedicalDocType docType;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 10) private MedicalDocStatus status;
    @Column(name = "document_date", nullable = false) private LocalDate documentDate;
    @Column(nullable = false, columnDefinition = "TEXT") private String content;
    @Column(name = "doctor_name_snapshot", length = 120) private String doctorNameSnapshot;
    @Column(name = "doctor_qualification_snapshot", length = 255) private String doctorQualificationSnapshot;
    @Column(name = "doctor_registration_snapshot", length = 80) private String doctorRegistrationSnapshot;
    @Column(name = "patient_name_snapshot", length = 120) private String patientNameSnapshot;
    @Column(name = "patient_code_snapshot", length = 20) private String patientCodeSnapshot;
    @Column(name = "issued_at") private LocalDateTime issuedAt;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "revision_of_id") private MedicalDocument revisionOf;
    @Column(name = "print_count", nullable = false) private int printCount;
}
