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
@Table(name = "bill_items")
public class BillItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "bill_id") private Bill bill;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private BillCategory category;
    @Column(nullable = false, length = 200) private String description;
    @Column(nullable = false) private int quantity;
    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2) private BigDecimal unitPrice;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal amount;
}
