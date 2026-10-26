package com.munot.hospital.util;

import com.munot.hospital.entity.MedicalDocType;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class MedicalDocumentRulesTest {
    private static Map<String, String> m(String... kv) { var map = new java.util.LinkedHashMap<String, String>(); for (int i = 0; i < kv.length; i += 2) map.put(kv[i], kv[i + 1]); return map; }
    private static String err(MedicalDocType t, Map<String, String> c, boolean issue) {
        try { MedicalDocumentRules.normalize(t, c, issue); return null; } catch (IllegalArgumentException e) { return e.getMessage(); }
    }

    @Test
    void draftsMayBeIncompleteButIssuingNeedsRequiredFields() {
        assertNull(err(MedicalDocType.MEDICAL_CERTIFICATE, m(), false));
        assertTrue(err(MedicalDocType.MEDICAL_CERTIFICATE, m(), true).contains("Medical condition / reason is required"));
        assertNull(err(MedicalDocType.MEDICAL_CERTIFICATE, m("condition", "Fracture left wrist"), true));
        assertTrue(err(MedicalDocType.ADMISSION_ADVICE, m("diagnosis", "ACL tear"), true).contains("Planned procedure is required"));
    }

    @Test
    void blankValuesAreDroppedAndTextIsTrimmedNothingIsInvented() {
        var out = MedicalDocumentRules.normalize(MedicalDocType.PHYSIOTHERAPY_REFERRAL, m("diagnosis", "  Frozen shoulder ", "duration", "   ", "instructions", ""), true);
        assertEquals(Map.of("diagnosis", "Frozen shoulder"), out);
        assertFalse(out.containsKey("duration"));          // no duration is ever made up
    }

    @Test
    void rejectsUnknownFieldsBadDatesAndDatesOutOfOrder() {
        assertTrue(err(MedicalDocType.MEDICAL_CERTIFICATE, m("condition", "x", "price", "100"), false).contains("Unknown field 'price'"));
        assertTrue(err(MedicalDocType.MEDICAL_CERTIFICATE, m("treatmentStart", "31-12-2026"), false).contains("not a valid date"));
        assertTrue(err(MedicalDocType.MEDICAL_CERTIFICATE, m("treatmentStart", "2026-10-10", "treatmentEnd", "2026-10-01"), false).contains("cannot be before the start date"));
        assertTrue(err(MedicalDocType.MEDICAL_CERTIFICATE, m("treatmentStart", "2026-10-10", "fitToResumeDate", "2026-10-09"), false).contains("fit-to-resume"));
        assertTrue(err(MedicalDocType.PHYSIOTHERAPY_REFERRAL, m("startDate", "2026-10-10", "endDate", "2026-10-09"), false).contains("end date cannot be before"));
        assertTrue(err(MedicalDocType.ADMISSION_ADVICE, m("admissionDate", "2026-10-10", "operationDate", "2026-10-09"), false).contains("operation date cannot be before"));
        assertNull(err(MedicalDocType.ADMISSION_ADVICE, m("admissionDate", "2026-10-10", "operationDate", "2026-10-10"), false));
    }

    @Test
    void implantCertificateKeepsFieldsOptionalButNotCompletelyEmptyWhenIssued() {
        assertNull(err(MedicalDocType.IMPLANT_CERTIFICATE, m(), false));
        assertTrue(err(MedicalDocType.IMPLANT_CERTIFICATE, m("notes", "n"), true).contains("at least the diagnosis, implant type or implant location"));
        assertNull(err(MedicalDocType.IMPLANT_CERTIFICATE, m("implantLocation", "Right knee"), true));
        assertNull(err(MedicalDocType.IMPLANT_CERTIFICATE, m("implantType", "Plate"), true));   // surgery date etc. stay optional
    }

    @Test
    void enforcesLengthLimits() {
        assertNotNull(err(MedicalDocType.MEDICAL_CERTIFICATE, m("restAdvised", "x".repeat(201)), false));
        assertNull(err(MedicalDocType.MEDICAL_CERTIFICATE, m("restAdvised", "x".repeat(200)), false));
    }

    @Test
    void latinCheckProtectsThePdfEngineAndMarginsAreRangeChecked() {
        assertTrue(MedicalDocumentRules.isLatin1(List.of("Asha Patil", "Fracture é")));
        assertFalse(MedicalDocumentRules.isLatin1(List.of("आशा")));
        assertTrue(MedicalDocumentRules.validMargin("print_margin_top_mm", 150));
        assertFalse(MedicalDocumentRules.validMargin("print_margin_top_mm", 151));
        assertTrue(MedicalDocumentRules.validMargin("print_margin_left_mm", 60));
        assertFalse(MedicalDocumentRules.validMargin("print_margin_right_mm", 61));
        assertFalse(MedicalDocumentRules.validMargin("print_margin_bottom_mm", -1));
    }

    @Test
    void everyDocumentTypeHasATitleAndFields() {
        for (MedicalDocType t : MedicalDocType.values()) {
            assertNotNull(MedicalDocumentRules.TITLES.get(t));
            assertTrue(MedicalDocumentRules.SPEC.get(t).size() >= 5);
        }
    }
}
