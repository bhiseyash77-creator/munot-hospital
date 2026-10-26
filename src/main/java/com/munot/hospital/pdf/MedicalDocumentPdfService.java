package com.munot.hospital.pdf;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import com.munot.hospital.common.ApiException;
import com.munot.hospital.dto.MedicalDocDtos.*;
import com.munot.hospital.entity.MedicalDocStatus;
import com.munot.hospital.entity.MedicalDocument;
import com.munot.hospital.entity.User;
import com.munot.hospital.service.MedicalDocumentService;
import com.munot.hospital.util.MedicalDocumentRules;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Content-only PDF for printing on the clinic's pre-printed letterhead: no logo, name, address, header, footer, border or background.
 * The blank area is set by the print margins (millimetres). Marathi/Hindi text is printed from the browser instead (see error message).
 */
@Service
@Transactional(readOnly = true)
public class MedicalDocumentPdfService {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final float MM = 72f / 25.4f;

    private final MedicalDocumentService docs;

    public MedicalDocumentPdfService(MedicalDocumentService docs) { this.docs = docs; }

    private static Font font(boolean bold, float size) { return FontFactory.getFont(bold ? FontFactory.HELVETICA_BOLD : FontFactory.HELVETICA, size, java.awt.Color.BLACK); }

    private static String date(String iso) { return LocalDate.parse(iso).format(DATE); }

    public byte[] pdf(User actor, Long id) {
        MedicalDocument entity = docs.readable(actor, id);
        if (entity.getStatus() != MedicalDocStatus.ISSUED) throw ApiException.conflict("Issue the document before printing it.");
        MedicalDocView v = docs.get(actor, id);
        PrintSettings m = docs.printSettings();

        List<String> texts = new ArrayList<>(v.content().values());
        texts.addAll(Arrays.asList(v.patientName(), v.patientCode(), v.doctorName(), v.doctorQualification(), v.doctorRegistrationNumber()));
        if (!MedicalDocumentRules.isLatin1(texts))
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "This document contains Marathi/Hindi characters, which the PDF engine cannot draw correctly. Use Print and choose 'Save as PDF' instead.");

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document doc = new Document(PageSize.A4, m.left() * MM, m.right() * MM, m.top() * MM, m.bottom() * MM);
            PdfWriter.getInstance(doc, out);
            doc.open();

            Paragraph title = new Paragraph(MedicalDocumentRules.TITLES.get(v.docType()), font(true, 13));
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(12);
            doc.add(title);

            Paragraph head = new Paragraph();
            head.add(new Chunk("Patient: ", font(true, 11)));
            head.add(new Chunk(v.patientName() + "  (Patient ID: " + v.patientCode() + ")", font(false, 11)));
            head.add(new Chunk("\nDate: ", font(true, 11)));
            head.add(new Chunk(v.documentDate().format(DATE), font(false, 11)));
            head.setSpacingAfter(10);
            doc.add(head);

            for (MedicalDocumentRules.Field f : MedicalDocumentRules.SPEC.get(v.docType())) {
                String value = v.content().get(f.key());
                if (value == null) continue;
                Paragraph p = new Paragraph();
                p.setSpacingAfter(7);
                p.setKeepTogether(true);
                p.add(new Chunk(f.label() + ": ", font(true, 11)));
                p.add(new Chunk(f.date() ? date(value) : value, font(false, 11)));
                doc.add(p);
            }

            Paragraph sign = new Paragraph("\n\n\n_______________________\nSignature", font(false, 10));
            sign.setAlignment(Element.ALIGN_RIGHT);
            sign.setKeepTogether(true);
            StringBuilder who = new StringBuilder("\n" + v.doctorName());
            if (v.doctorQualification() != null && !v.doctorQualification().isBlank()) who.append("\n").append(v.doctorQualification());
            if (v.doctorRegistrationNumber() != null && !v.doctorRegistrationNumber().isBlank()) who.append("\nReg. No: ").append(v.doctorRegistrationNumber());
            sign.add(new Chunk(who.toString(), font(true, 10)));
            doc.add(sign);

            doc.close();
            docs.markPrinted(actor, id);
            return out.toByteArray();
        } catch (DocumentException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not generate PDF");
        }
    }
}
