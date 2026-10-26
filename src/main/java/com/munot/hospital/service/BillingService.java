package com.munot.hospital.service;

import com.munot.hospital.common.ApiException;
import com.munot.hospital.dto.BillingDtos.*;
import com.munot.hospital.entity.*;
import com.munot.hospital.repository.*;
import com.munot.hospital.util.BillMath;
import com.munot.hospital.util.Codes;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BillingService {
    private final BillRepository bills;
    private final PaymentRepository payments;
    private final PatientRepository patients;
    private final AppointmentRepository appointments;
    private final AccessControl access;
    private final ViewMapper mapper;
    private final AuditService audit;

    @Transactional
    public BillView create(BillRequest req, User actor) {
        Patient patient = patients.findById(req.patientId()).orElseThrow(() -> ApiException.notFound("Patient not found"));
        Appointment appt = null;
        if (req.appointmentId() != null) {
            appt = appointments.findById(req.appointmentId()).orElseThrow(() -> ApiException.notFound("Appointment not found"));
            if (!appt.getPatient().getId().equals(patient.getId())) throw ApiException.badRequest("Appointment does not belong to this patient");
            if (bills.findByAppointmentId(appt.getId()).isPresent()) throw ApiException.conflict("A bill already exists for this appointment.");
        }
        Bill b = new Bill();
        b.setBillNumber("TMP-" + UUID.randomUUID().toString().substring(0, 12));
        b.setPatient(patient);
        b.setAppointment(appt);
        b.setCreatedBy(actor);
        b.setNotes(req.notes());
        List<BigDecimal> lines = new ArrayList<>();
        for (BillItemRequest r : req.items()) {
            BillItem i = new BillItem();
            i.setBill(b);
            i.setCategory(r.category());
            i.setDescription(r.description());
            i.setQuantity(r.quantity());
            i.setUnitPrice(BillMath.money(r.unitPrice()));
            i.setAmount(BillMath.money(r.unitPrice().multiply(BigDecimal.valueOf(r.quantity()))));
            lines.add(i.getAmount());
            b.getItems().add(i);
        }
        BillMath.Totals t;
        try {
            t = BillMath.compute(lines, req.discount(), req.taxPercent());
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest(e.getMessage());
        }
        b.setSubtotal(t.subtotal());
        b.setDiscount(t.discount());
        b.setTaxPercent(req.taxPercent() == null ? BigDecimal.ZERO : req.taxPercent());
        b.setTax(t.tax());
        b.setTotal(t.total());
        b.setPaidAmount(BigDecimal.ZERO);
        b.setPendingAmount(t.total());
        b.setStatus(BillMath.status(t.total(), BigDecimal.ZERO));
        b = bills.saveAndFlush(b);
        b.setBillNumber(Codes.of("INV", b.getId()));
        audit.log("BILL_CREATED", "Bill", b.getId(), b.getBillNumber() + " total=" + b.getTotal());
        if (req.initialPayment() != null) applyPayment(b, req.initialPayment(), actor);
        return mapper.bill(b);
    }

    @Transactional
    public BillView addPayment(Long billId, PaymentRequest req, User actor) {
        Bill b = bills.findByIdForUpdate(billId).orElseThrow(() -> ApiException.notFound("Bill not found"));
        applyPayment(b, req, actor);
        return mapper.bill(b);
    }

    private void applyPayment(Bill b, PaymentRequest req, User actor) {
        BigDecimal amount = BillMath.money(req.amount());
        if (amount.compareTo(b.getPendingAmount()) > 0) throw ApiException.conflict("Payment exceeds the pending amount (" + b.getPendingAmount() + ").");
        Payment p = new Payment();
        p.setBill(b);
        p.setAmount(amount);
        p.setMethod(req.method());
        p.setReference(req.reference());
        p.setPaidAt(LocalDateTime.now());
        p.setReceivedBy(actor);
        payments.save(p);
        b.getPayments().add(p);
        b.setPaidAmount(b.getPaidAmount().add(amount));
        b.setPendingAmount(b.getTotal().subtract(b.getPaidAmount()));
        b.setStatus(BillMath.status(b.getTotal(), b.getPaidAmount()));
        bills.save(b);
        audit.log("PAYMENT_RECORDED", "Bill", b.getId(), amount + " via " + req.method());
    }

    public Bill requireBill(Long id, User actor) {
        Bill b = bills.findById(id).orElseThrow(() -> ApiException.notFound("Bill not found"));
        switch (actor.getRole()) {
            case PATIENT -> { if (b.getPatient().getUser() == null || !b.getPatient().getUser().getId().equals(actor.getId())) throw ApiException.forbidden("Access denied"); }
            default -> { }
        }
        return b;
    }

    public BillView get(Long id, User actor) { return mapper.bill(requireBill(id, actor)); }

    public List<BillView> mine(User actor) {
        Patient p = access.patientOf(actor);
        return bills.findByPatientIdOrderByCreatedAtDesc(p.getId()).stream().map(mapper::bill).toList();
    }

    public List<BillView> forPatient(Long patientId) {
        return bills.findByPatientIdOrderByCreatedAtDesc(patientId).stream().map(mapper::bill).toList();
    }

    public List<BillView> recent(boolean pendingOnly) {
        List<Bill> list = pendingOnly ? bills.findByStatusInOrderByCreatedAtDesc(List.of(PaymentStatus.PENDING, PaymentStatus.PARTIAL))
                : bills.findTop50ByOrderByCreatedAtDesc();
        return list.stream().map(mapper::bill).toList();
    }
}
