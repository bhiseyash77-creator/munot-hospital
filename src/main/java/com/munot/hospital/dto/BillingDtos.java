package com.munot.hospital.dto;

import com.munot.hospital.entity.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public final class BillingDtos {
    private BillingDtos() {}

    public record BillItemRequest(@NotNull BillCategory category, @NotBlank @Size(max = 200) String description,
                                  @Min(1) @Max(1000) int quantity, @NotNull @DecimalMin("0.00") BigDecimal unitPrice) {}

    public record PaymentRequest(@NotNull @DecimalMin("0.01") BigDecimal amount, @NotNull PaymentMethod method,
                                 @Size(max = 100) String reference) {}

    public record BillRequest(@NotNull Long patientId, Long appointmentId, @NotEmpty @Valid List<BillItemRequest> items,
                              @DecimalMin("0.00") BigDecimal discount, @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal taxPercent,
                              @Size(max = 300) String notes, @Valid PaymentRequest initialPayment) {}

    public record BillItemView(Long id, BillCategory category, String description, int quantity, BigDecimal unitPrice, BigDecimal amount) {}

    public record PaymentView(Long id, BigDecimal amount, PaymentMethod method, String reference, LocalDateTime paidAt) {}

    public record BillView(Long id, String billNumber, Long patientId, String patientName, String patientCode, Long appointmentId,
                           List<BillItemView> items, BigDecimal subtotal, BigDecimal discount, BigDecimal taxPercent, BigDecimal tax,
                           BigDecimal total, BigDecimal paid, BigDecimal pending, PaymentStatus status, String notes,
                           LocalDateTime createdAt, List<PaymentView> payments) {}
}
