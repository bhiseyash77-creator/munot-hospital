package com.munot.hospital.util;

import com.munot.hospital.entity.PaymentStatus;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public final class BillMath {
    private BillMath() {}

    public record Totals(BigDecimal subtotal, BigDecimal discount, BigDecimal tax, BigDecimal total) {}

    public static BigDecimal money(BigDecimal v) { return v.setScale(2, RoundingMode.HALF_UP); }

    public static Totals compute(List<BigDecimal> lineAmounts, BigDecimal discount, BigDecimal taxPercent) {
        BigDecimal subtotal = money(lineAmounts.stream().reduce(BigDecimal.ZERO, BigDecimal::add));
        BigDecimal disc = money(discount == null ? BigDecimal.ZERO : discount);
        if (disc.signum() < 0 || disc.compareTo(subtotal) > 0) throw new IllegalArgumentException("Discount must be between 0 and the subtotal");
        BigDecimal pct = taxPercent == null ? BigDecimal.ZERO : taxPercent;
        if (pct.signum() < 0 || pct.compareTo(BigDecimal.valueOf(100)) > 0) throw new IllegalArgumentException("Tax percent must be between 0 and 100");
        BigDecimal taxable = subtotal.subtract(disc);
        BigDecimal tax = money(taxable.multiply(pct).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
        return new Totals(subtotal, disc, tax, money(taxable.add(tax)));
    }

    public static PaymentStatus status(BigDecimal total, BigDecimal paid) {
        if (paid.compareTo(total) >= 0) return PaymentStatus.PAID;
        return paid.signum() > 0 ? PaymentStatus.PARTIAL : PaymentStatus.PENDING;
    }
}
