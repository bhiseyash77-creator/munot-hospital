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
@Table(name = "notifications")
public class Notification extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "recipient_user_id") private User recipientUser;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "patient_id") private Patient patient;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "appointment_id") private Appointment appointment;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 12) private Channel channel;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private NotificationType type;
    @Column(nullable = false, length = 200) private String title;
    @Column(nullable = false, length = 1000) private String message;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 12) private NotificationStatus status;
    @Column(name = "scheduled_at", nullable = false) private LocalDateTime scheduledAt;
    @Column(name = "sent_at") private LocalDateTime sentAt;
    @Column(name = "read_flag", nullable = false) private boolean readFlag;
    @Column(nullable = false) private int attempts;
    @Column(name = "last_error", length = 300) private String lastError;
    @Column(name = "dedupe_key", nullable = false, length = 120) private String dedupeKey;
}
