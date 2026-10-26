package com.munot.hospital.util;

import com.munot.hospital.entity.MedicalDocType;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Field definitions and validation for the four doctor-issued documents. Pure Java (unit-tested).
 * Drafts only need well-formed values; issuing also needs every required field. Nothing is ever filled in automatically.
 */
public final class MedicalDocumentRules {
    private MedicalDocumentRules() {}

    public record Field(String key, String label, boolean required, boolean date, int max) {}

    private static Field t(String key, String label, boolean req, int max) { return new Field(key, label, req, false, max); }
    private static Field d(String key, String label) { return new Field(key, label, false, true, 10); }

    public static final Map<MedicalDocType, String> TITLES = Map.of(
            MedicalDocType.MEDICAL_CERTIFICATE, "MEDICAL CERTIFICATE",
            MedicalDocType.PHYSIOTHERAPY_REFERRAL, "PHYSIOTHERAPY REFERRAL",
            MedicalDocType.IMPLANT_CERTIFICATE, "IMPLANT CERTIFICATE",
            MedicalDocType.ADMISSION_ADVICE, "ADMISSION / OPERATION ADVICE");

    public static final Map<MedicalDocType, List<Field>> SPEC = Map.of(
            MedicalDocType.MEDICAL_CERTIFICATE, List.of(
                    t("condition", "Medical condition / reason", true, 2000), d("treatmentStart", "Treatment start date"), d("treatmentEnd", "Treatment end date"),
                    t("restAdvised", "Rest advised", false, 200), d("fitToResumeDate", "Fit to resume work / regular duties on"), t("remarks", "Remarks", false, 1000)),
            MedicalDocType.PHYSIOTHERAPY_REFERRAL, List.of(
                    t("diagnosis", "Diagnosis / clinical reason", true, 1000), d("startDate", "Physiotherapy start date"), t("duration", "Recommended duration", false, 200),
                    d("endDate", "Physiotherapy end date"), t("instructions", "Treatment instructions", false, 3000)),
            MedicalDocType.IMPLANT_CERTIFICATE, List.of(
                    t("diagnosis", "Diagnosis / medical reason", false, 1000), t("implantType", "Implant type", false, 300), t("implantLocation", "Implant location", false, 300),
                    t("implantDetails", "Implant details", false, 1500), d("surgeryDate", "Surgery date"), t("notes", "Relevant medical notes", false, 2000)),
            MedicalDocType.ADMISSION_ADVICE, List.of(
                    t("diagnosis", "Diagnosis / reason for admission", true, 1000), t("plannedProcedure", "Planned procedure", true, 1000), d("admissionDate", "Hospital admission date"),
                    d("operationDate", "Proposed operation date"), t("anaesthesiaAssessment", "Anaesthesia assessment / fitness instructions", false, 1500),
                    t("nbmInstructions", "NBM instructions", false, 1500), t("nbmTime", "NBM from (time)", false, 100), t("otherInstructions", "Other pre-operative instructions", false, 3000)));

    /** At least one of these must be filled for an implant certificate (an empty certificate is meaningless). */
    private static final List<String> IMPLANT_ANY_OF = List.of("diagnosis", "implantType", "implantLocation");

    private static LocalDate date(String v) { return LocalDate.parse(v); }

    /** Returns the cleaned content (blank values dropped, text trimmed). Throws IllegalArgumentException listing every problem. */
    public static Map<String, String> normalize(MedicalDocType type, Map<String, String> in, boolean forIssue) {
        List<Field> spec = SPEC.get(type);
        List<String> errors = new ArrayList<>();
        Map<String, String> out = new LinkedHashMap<>();
        Set<String> known = new HashSet<>();
        spec.forEach(f -> known.add(f.key()));
        Map<String, String> src = in == null ? Map.of() : in;
        for (String k : src.keySet()) if (!known.contains(k)) errors.add("Unknown field '" + k + "'");
        for (Field f : spec) {
            String v = src.get(f.key());
            v = v == null ? "" : v.trim().replace("\r\n", "\n");
            if (v.isEmpty()) {
                if (forIssue && f.required()) errors.add(f.label() + " is required");
                continue;
            }
            if (v.length() > f.max()) { errors.add(f.label() + " is too long (max " + f.max() + " characters)"); continue; }
            if (f.date()) {
                try { date(v); } catch (DateTimeParseException e) { errors.add(f.label() + " is not a valid date"); continue; }
            }
            out.put(f.key(), v);
        }
        if (forIssue && type == MedicalDocType.IMPLANT_CERTIFICATE && IMPLANT_ANY_OF.stream().noneMatch(out::containsKey))
            errors.add("Enter at least the diagnosis, implant type or implant location");
        before(errors, out, "treatmentStart", "treatmentEnd", "Treatment end date cannot be before the start date");
        before(errors, out, "treatmentStart", "fitToResumeDate", "The fit-to-resume date cannot be before the treatment start date");
        before(errors, out, "startDate", "endDate", "Physiotherapy end date cannot be before the start date");
        before(errors, out, "admissionDate", "operationDate", "The operation date cannot be before the admission date");
        if (!errors.isEmpty()) throw new IllegalArgumentException(String.join("; ", errors));
        return out;
    }

    private static void before(List<String> errors, Map<String, String> m, String first, String second, String message) {
        if (m.containsKey(first) && m.containsKey(second) && date(m.get(second)).isBefore(date(m.get(first)))) errors.add(message);
    }

    /** The server-side PDF uses a Latin font; Marathi/Hindi text must be printed from the browser instead. */
    public static boolean isLatin1(Collection<String> texts) {
        for (String s : texts) if (s != null) for (int i = 0; i < s.length(); i++) if (s.charAt(i) > 0xFF) return false;
        return true;
    }

    /** Clamp-check for the printable-area margins (millimetres). */
    public static boolean validMargin(String key, int mm) {
        return key.endsWith("_top_mm") || key.endsWith("_bottom_mm") ? mm >= 0 && mm <= 150 : mm >= 0 && mm <= 60;
    }
}
