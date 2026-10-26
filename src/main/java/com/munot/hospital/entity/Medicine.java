package com.munot.hospital.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "medicines")
public class Medicine extends BaseEntity {
    @Column(nullable = false, length = 200) private String name;
    @Column(length = 300) private String composition;
    @Column(name = "use_count", nullable = false) private int useCount;
    @Column(name = "last_used_at") private LocalDateTime lastUsedAt;
}
