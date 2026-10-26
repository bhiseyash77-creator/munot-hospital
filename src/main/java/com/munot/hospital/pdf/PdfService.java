package com.munot.hospital.pdf;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.munot.hospital.common.ApiException;
import com.munot.hospital.entity.*;
import com.munot.hospital.service.BillingService;
import com.munot.hospital.service.ConsultationService;
import com.munot.hospital.service.SettingsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Service
@Transactional(readOnly = true)
public class PdfService {
    private static final Color TEAL = new Color(14, 116, 144);
    private static final Color GREY = new Color(100, 116, 139);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter DATETIME = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    private final ConsultationService consultations;
    private final BillingService billing;
    private final SettingsService settings;

    public PdfService(ConsultationService consultations, BillingService billing, SettingsService settings) {
        this.consultations = consultations;
        this.billing = billing;
        this.settings = settings;
    }

    private static Font f(boolean bold, float size, Color c) {
        return FontFactory.getFont(bold ? FontFactory.HELVETICA_BOLD : FontFactory.HELVETICA, size, c);
    }

    private void letterhead(Document doc) throws DocumentException {
        Paragraph name = new Paragraph(settings.get("hospital_name", "Hospital"), f(true, 18, TEAL));
        name.setAlignment(Element.ALIGN_CENTER);
        doc.add(name);
        Paragraph addr = new Paragraph(settings.get("address", "") + "\nPhone: " + settings.get("phone", "") + "   Email: " + settings.get("email", ""), f(false, 9, GREY));
        addr.setAlignment(Element.ALIGN_CENTER);
        addr.setSpacingAfter(6);
        doc.add(addr);
        PdfPTable line = new PdfPTable(1);
        line.setWidthPercentage(100);
        PdfPCell c = new PdfPCell(new Phrase(" "));
        c.setBorder(Rectangle.BOTTOM);
        c.setBorderColor(TEAL);
        c.setBorderWidth(1.5f);
        line.addCell(c);
        line.setSpacingAfter(8);
        doc.add(line);
    }

    private static PdfPCell cell(String text, Font font, int border, int align) {
        PdfPCell c = new PdfPCell(new Phrase(text == null ? "" : text, font));
        c.setBorder(border);
        c.setBorderColor(new Color(203, 213, 225));
        c.setPadding(5);
        c.setHorizontalAlignment(align);
        return c;
    }

    private static PdfPCell head(String text) {
        PdfPCell c = cell(text, f(true, 9, Color.WHITE), Rectangle.NO_BORDER, Element.ALIGN_LEFT);
        c.setBackgroundColor(TEAL);
        return c;
    }

    public byte[] prescriptionPdf(Long id, User actor) {
        Prescription rx = consultations.requirePrescription(id, actor);
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document doc = new Document(PageSize.A4, 40, 40, 36, 48);
            PdfWriter.getInstance(doc, out);
            doc.open();
            letterhead(doc);
            Patient p = rx.getPatient();
            Doctor d = rx.getDoctor();

            PdfPTable info = new PdfPTable(new float[]{1, 1});
            info.setWidthPercentage(100);
            info.addCell(cell("Patient: " + p.getFullName() + "  (" + p.getPatientCode() + ")", f(true, 10, Color.BLACK), Rectangle.NO_BORDER, Element.ALIGN_LEFT));
            info.addCell(cell("Prescription: " + rx.getPrescriptionCode(), f(true, 10, Color.BLACK), Rectangle.NO_BORDER, Element.ALIGN_RIGHT));
            String ageGender = (p.getDateOfBirth() != null ? "DOB: " + p.getDateOfBirth().format(DATE) + "   " : "") + (p.getGender() != null ? "Gender: " + p.getGender() : "");
            info.addCell(cell(ageGender, f(false, 9, GREY), Rectangle.NO_BORDER, Element.ALIGN_LEFT));
            info.addCell(cell("Date: " + rx.getCreatedAt().format(DATETIME), f(false, 9, GREY), Rectangle.NO_BORDER, Element.ALIGN_RIGHT));
            doc.add(info);

            if (!rx.getConsultation().getDiagnoses().isEmpty()) {
                StringBuilder sb = new StringBuilder();
                rx.getConsultation().getDiagnoses().forEach(x -> sb.append(sb.length() > 0 ? "; " : "").append(x.getName()));
                Paragraph dx = new Paragraph();
                dx.setSpacingBefore(8);
                dx.add(new Chunk("Diagnosis: ", f(true, 10, Color.BLACK)));
                dx.add(new Chunk(sb.toString(), f(false, 10, Color.BLACK)));
                doc.add(dx);
            }
            Paragraph rxMark = new Paragraph("Rx", f(true, 16, TEAL));
            rxMark.setSpacingBefore(8);
            rxMark.setSpacingAfter(4);
            doc.add(rxMark);

            PdfPTable t = new PdfPTable(new float[]{0.5f, 3.6f, 1.3f, 1.5f, 1.3f, 2.3f});
            t.setWidthPercentage(100);
            for (String h : new String[]{"#", "Medicine (composition)", "Dosage", "Frequency", "Duration", "Instructions"}) t.addCell(head(h));
            int n = 1;
            for (PrescriptionItem i : rx.getItems()) {
                Font body = f(false, 9, Color.BLACK);
                t.addCell(cell(String.valueOf(n++), body, Rectangle.BOTTOM, Element.ALIGN_LEFT));
                Phrase nameCell = new Phrase();
                nameCell.add(new Chunk(i.getMedicineName(), f(true, 9, Color.BLACK)));
                if (i.getComposition() != null && !i.getComposition().isBlank()) nameCell.add(new Chunk("\n" + i.getComposition(), f(false, 8, GREY)));
                PdfPCell medCell = new PdfPCell(nameCell);
                medCell.setBorder(Rectangle.BOTTOM);
                medCell.setBorderColor(new Color(203, 213, 225));
                medCell.setPadding(5);
                t.addCell(medCell);
                t.addCell(cell(i.getDosage(), body, Rectangle.BOTTOM, Element.ALIGN_LEFT));
                t.addCell(cell(i.getFrequency(), body, Rectangle.BOTTOM, Element.ALIGN_LEFT));
                t.addCell(cell(i.getDuration(), body, Rectangle.BOTTOM, Element.ALIGN_LEFT));
                t.addCell(cell(i.getInstructions(), body, Rectangle.BOTTOM, Element.ALIGN_LEFT));
            }
            doc.add(t);

            Consultation c = rx.getConsultation();
            addBlock(doc, "Tests advised", c.getTestsAdvised());
            addBlock(doc, "Advice", c.getAdvice());
            addBlock(doc, "Notes", rx.getNotes());

            Paragraph sign = new Paragraph("\n\n" + d.getUser().getFullName()
                    + (d.getQualification() != null && !d.getQualification().isBlank() ? "\n" + d.getQualification() : "")
                    + (d.getRegistrationNumber() != null && !d.getRegistrationNumber().isBlank() ? "\nReg. No: " + d.getRegistrationNumber() : ""),
                    f(true, 10, Color.BLACK));
            sign.setAlignment(Element.ALIGN_RIGHT);
            doc.add(sign);
            Paragraph foot = new Paragraph("\nThis is a computer generated prescription. Please follow the advice of your doctor.", f(false, 8, GREY));
            foot.setAlignment(Element.ALIGN_CENTER);
            doc.add(foot);
            doc.close();
            return out.toByteArray();
        } catch (DocumentException e) {
            throw new ApiException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "Could not generate PDF");
        }
    }

    private void addBlock(Document doc, String title, String text) throws DocumentException {
        if (text == null || text.isBlank()) return;
        Paragraph p = new Paragraph();
        p.setSpacingBefore(8);
        p.add(new Chunk(title + ": ", f(true, 10, Color.BLACK)));
        p.add(new Chunk(text, f(false, 10, Color.BLACK)));
        doc.add(p);
    }

    public byte[] invoicePdf(Long billId, User actor) {
        Bill b = billing.requireBill(billId, actor);
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document doc = new Document(PageSize.A4, 40, 40, 36, 48);
            PdfWriter.getInstance(doc, out);
            doc.open();
            letterhead(doc);
            Patient p = b.getPatient();
            Paragraph title = new Paragraph("INVOICE", f(true, 14, Color.BLACK));
            title.setSpacingAfter(4);
            doc.add(title);
            PdfPTable info = new PdfPTable(new float[]{1, 1});
            info.setWidthPercentage(100);
            info.addCell(cell("Billed to: " + p.getFullName() + " (" + p.getPatientCode() + ")\nPhone: " + p.getPhone(), f(false, 10, Color.BLACK), Rectangle.NO_BORDER, Element.ALIGN_LEFT));
            info.addCell(cell("Invoice No: " + b.getBillNumber() + "\nDate: " + b.getCreatedAt().format(DATE) + "\nStatus: " + b.getStatus(), f(false, 10, Color.BLACK), Rectangle.NO_BORDER, Element.ALIGN_RIGHT));
            doc.add(info);

            PdfPTable t = new PdfPTable(new float[]{3.5f, 1.6f, 0.8f, 1.4f, 1.6f});
            t.setWidthPercentage(100);
            t.setSpacingBefore(8);
            for (String h : new String[]{"Description", "Category", "Qty", "Rate (Rs.)", "Amount (Rs.)"}) t.addCell(head(h));
            Font body = f(false, 9, Color.BLACK);
            for (BillItem i : b.getItems()) {
                t.addCell(cell(i.getDescription(), body, Rectangle.BOTTOM, Element.ALIGN_LEFT));
                t.addCell(cell(i.getCategory().name(), body, Rectangle.BOTTOM, Element.ALIGN_LEFT));
                t.addCell(cell(String.valueOf(i.getQuantity()), body, Rectangle.BOTTOM, Element.ALIGN_LEFT));
                t.addCell(cell(i.getUnitPrice().toPlainString(), body, Rectangle.BOTTOM, Element.ALIGN_RIGHT));
                t.addCell(cell(i.getAmount().toPlainString(), body, Rectangle.BOTTOM, Element.ALIGN_RIGHT));
            }
            doc.add(t);

            PdfPTable tot = new PdfPTable(new float[]{3, 1.5f});
            tot.setWidthPercentage(45);
            tot.setHorizontalAlignment(Element.ALIGN_RIGHT);
            tot.setSpacingBefore(8);
            row(tot, "Subtotal", b.getSubtotal().toPlainString(), false);
            row(tot, "Discount", "-" + b.getDiscount().toPlainString(), false);
            row(tot, "Tax (" + b.getTaxPercent().stripTrailingZeros().toPlainString() + "%)", b.getTax().toPlainString(), false);
            row(tot, "Total", b.getTotal().toPlainString(), true);
            row(tot, "Paid", b.getPaidAmount().toPlainString(), false);
            row(tot, "Pending", b.getPendingAmount().toPlainString(), true);
            doc.add(tot);

            if (!b.getPayments().isEmpty()) {
                Paragraph ph = new Paragraph("Payments", f(true, 10, Color.BLACK));
                ph.setSpacingBefore(10);
                doc.add(ph);
                for (Payment pay : b.getPayments()) {
                    doc.add(new Paragraph(pay.getPaidAt().format(DATETIME) + "  -  Rs. " + pay.getAmount().toPlainString() + "  (" + pay.getMethod()
                            + (pay.getReference() != null && !pay.getReference().isBlank() ? ", ref " + pay.getReference() : "") + ")", f(false, 9, Color.BLACK)));
                }
            }
            Paragraph foot = new Paragraph("\n" + settings.get("invoice_footer", ""), f(false, 8, GREY));
            foot.setAlignment(Element.ALIGN_CENTER);
            doc.add(foot);
            doc.close();
            return out.toByteArray();
        } catch (DocumentException e) {
            throw new ApiException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "Could not generate PDF");
        }
    }

    private static void row(PdfPTable t, String label, String value, boolean bold) {
        Font font = f(bold, 10, Color.BLACK);
        t.addCell(cell(label, font, Rectangle.NO_BORDER, Element.ALIGN_LEFT));
        t.addCell(cell(value, font, Rectangle.NO_BORDER, Element.ALIGN_RIGHT));
    }
}
