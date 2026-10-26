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
@Table(name = "audit_logs")
public class AuditLog extends BaseEntity {

    @Column(name = "user_id") private Long userId;
    @Column(length = 150) private String username;
    @Column(nullable = false, length = 60) private String action;
    @Column(name = "entity_type", length = 60) private String entityType;
    @Column(name = "entity_id", length = 60) private String entityId;
    @Column(length = 500) private String details;
    @Column(name = "ip_address", length = 60) private String ipAddress;
}
