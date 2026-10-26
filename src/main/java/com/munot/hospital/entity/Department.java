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
@Table(name = "departments")
public class Department extends BaseEntity {

    @Column(nullable = false, length = 100) private String name;
    @Column(length = 500) private String description;
}
