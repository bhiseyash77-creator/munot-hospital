package com.munot.hospital.util;

import com.munot.hospital.entity.PaymentStatus;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class BillMathTest {
    @Test
    void computesTotalsWithDiscountAndTax() {
        var t = BillMath.compute(List.of(new BigDecimal("500.00"), new BigDecimal("300.00")), new BigDecimal("50"), new BigDecimal("5"));
        assertEquals(new BigDecimal("800.00"), t.subtotal());
        assertEquals(new BigDecimal("37.50"), t.tax());
        assertEquals(new BigDecimal("787.50"), t.total());
    }

    @Test
    void derivesPaymentStatus() {
        BigDecimal total = new BigDecimal("787.50");
        assertEquals(PaymentStatus.PENDING, BillMath.status(total, BigDecimal.ZERO));
        assertEquals(PaymentStatus.PARTIAL, BillMath.status(total, new BigDecimal("500")));
        assertEquals(PaymentStatus.PAID, BillMath.status(total, total));
    }

    @Test
    void rejectsDiscountAboveSubtotal() {
        assertThrows(IllegalArgumentException.class, () -> BillMath.compute(List.of(new BigDecimal("100")), new BigDecimal("101"), null));
    }
}
