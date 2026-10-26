package com.munot.hospital.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "clinic_photos")
public class ClinicPhoto extends BaseEntity {
    @Column(length = 200) private String caption;
    @Column(name = "stored_name", nullable = false, length = 100) private String storedName;
    @Column(name = "content_type", nullable = false, length = 50) private String contentType;
    @Column(name = "sort_order", nullable = false) private int sortOrder;
    @Column(nullable = false) private boolean active = true;
}
