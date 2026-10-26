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
@Table(name = "bills")
public class Bill extends BaseEntity {

    @Column(name = "bill_number", nullable = false, length = 30) private String billNumber;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "patient_id") private Patient patient;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "appointment_id") private Appointment appointment;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal subtotal;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal discount = BigDecimal.ZERO;
    @Column(name = "tax_percent", nullable = false, precision = 5, scale = 2) private BigDecimal taxPercent = BigDecimal.ZERO;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal tax = BigDecimal.ZERO;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal total;
    @Column(name = "paid_amount", nullable = false, precision = 12, scale = 2) private BigDecimal paidAmount = BigDecimal.ZERO;
    @Column(name = "pending_amount", nullable = false, precision = 12, scale = 2) private BigDecimal pendingAmount;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 10) private PaymentStatus status;
    @Column(length = 300) private String notes;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "created_by") private User createdBy;
    @OneToMany(mappedBy = "bill", cascade = CascadeType.ALL, orphanRemoval = true) private List<BillItem> items = new ArrayList<>();
    @OneToMany(mappedBy = "bill") @OrderBy("paidAt ASC") private List<Payment> payments = new ArrayList<>();
}
