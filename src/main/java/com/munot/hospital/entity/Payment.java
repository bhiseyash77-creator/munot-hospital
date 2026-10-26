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
@Table(name = "payments")
public class Payment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "bill_id") private Bill bill;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal amount;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private PaymentMethod method;
    @Column(length = 100) private String reference;
    @Column(name = "paid_at", nullable = false) private LocalDateTime paidAt;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "received_by") private User receivedBy;
}
