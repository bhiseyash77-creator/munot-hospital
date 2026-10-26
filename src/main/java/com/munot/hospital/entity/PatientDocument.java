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
@Table(name = "patient_documents")
public class PatientDocument extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "patient_id") private Patient patient;
    @Column(name = "file_name", nullable = false, length = 255) private String fileName;
    @Column(name = "stored_name", nullable = false, length = 100) private String storedName;
    @Column(name = "content_type", nullable = false, length = 100) private String contentType;
    @Column(name = "size_bytes", nullable = false) private long sizeBytes;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "uploaded_by") private User uploadedBy;
}
