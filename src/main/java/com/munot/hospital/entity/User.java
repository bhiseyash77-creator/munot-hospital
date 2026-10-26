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
@Table(name = "users")
public class User extends BaseEntity {

    @Column(nullable = false, length = 150) private String email;
    @Column(length = 20) private String phone;
    @Column(name = "password_hash", nullable = false, length = 100) private String passwordHash;
    @Column(name = "full_name", nullable = false, length = 120) private String fullName;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private Role role;
    @Column(nullable = false) private boolean enabled = true;
}
