package com.munot.hospital.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.munot.hospital.common.ApiException;
import com.munot.hospital.dto.MedicalDocDtos.*;
import com.munot.hospital.entity.*;
import com.munot.hospital.repository.MedicalDocumentRepository;
import com.munot.hospital.util.Codes;
import com.munot.hospital.util.MedicalDocumentRules;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/** Doctor-issued documents: draft -> issued (read-only) -> revise (new draft). Only the issuing doctor changes a document. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MedicalDocumentService {
    private final MedicalDocumentRepository docs;
    private final AccessControl access;
    private final SettingsService settings;
    private final ObjectMapper json;
    private final AuditService audit;

    // ---------- mapping ----------

    private String write(Map<String, String> content) {
        try { return json.writeValueAsString(content); } catch (JsonProcessingException e) { throw new IllegalStateException(e); }
    }

    public Map<String, String> read(MedicalDocument d) {
        try { return json.readValue(d.getContent(), new TypeReference<LinkedHashMap<String, String>>() {}); } catch (JsonProcessingException e) { throw new IllegalStateException(e); }
    }

    private MedicalDocView view(MedicalDocument d) {
        boolean issued = d.getStatus() == MedicalDocStatus.ISSUED;
        Doctor doc = d.getDoctor();
        return new MedicalDocView(d.getId(), d.getDocCode(), d.getDocType(), d.getStatus(), d.getDocumentDate(), d.getPatient().getId(),
                issued && d.getPatientNameSnapshot() != null ? d.getPatientNameSnapshot() : d.getPatient().getFullName(),
                issued && d.getPatientCodeSnapshot() != null ? d.getPatientCodeSnapshot() : d.getPatient().getPatientCode(), doc.getId(),
                issued && d.getDoctorNameSnapshot() != null ? d.getDoctorNameSnapshot() : doc.getUser().getFullName(),
                issued ? d.getDoctorQualificationSnapshot() : doc.getQualification(),
                issued ? d.getDoctorRegistrationSnapshot() : doc.getRegistrationNumber(),
                read(d), d.getIssuedAt(), d.getRevisionOf() == null ? null : d.getRevisionOf().getId(), d.getPrintCount(), d.getCreatedAt(), d.getUpdatedAt());
    }

    private Map<String, String> clean(MedicalDocType type, Map<String, String> content, boolean forIssue) {
        try { return MedicalDocumentRules.normalize(type, content, forIssue); } catch (IllegalArgumentException e) { throw ApiException.badRequest(e.getMessage()); }
    }

    /** Readable by a doctor who treats the patient. */
    public MedicalDocument readable(User actor, Long id) {
        MedicalDocument d = docs.findById(id).orElseThrow(() -> ApiException.notFound("Document not found"));
        access.requirePatient(actor, d.getPatient().getId());
        return d;
    }

    private MedicalDocument owned(User actor, Long id) {
        MedicalDocument d = docs.findById(id).orElseThrow(() -> ApiException.notFound("Document not found"));
        if (actor.getRole() != Role.DOCTOR || !d.getDoctor().getUser().getId().equals(actor.getId())) throw ApiException.forbidden("Only the issuing doctor can change this document");
        return d;
    }

    // ---------- operations ----------

    @Transactional
    public MedicalDocView create(User actor, CreateRequest r) {
        Doctor doctor = access.doctorOf(actor);
        Patient patient = access.requirePatient(actor, r.patientId());
        MedicalDocument d = new MedicalDocument();
        d.setDocCode("TMP-" + UUID.randomUUID().toString().substring(0, 12));
        d.setPatient(patient);
        d.setDoctor(doctor);
        d.setDocType(r.docType());
        d.setStatus(MedicalDocStatus.DRAFT);
        d.setDocumentDate(r.documentDate());
        d.setContent(write(clean(r.docType(), r.content(), false)));
        d = docs.saveAndFlush(d);
        d.setDocCode(Codes.of("MD", d.getId()));
        audit.log("MEDICAL_DOC_DRAFT_CREATED", "MedicalDocument", d.getId(), d.getDocType().name());
        return view(d);
    }

    @Transactional
    public MedicalDocView update(User actor, Long id, UpdateRequest r) {
        MedicalDocument d = owned(actor, id);
        if (d.getStatus() != MedicalDocStatus.DRAFT) throw ApiException.conflict("An issued document cannot be edited. Use Revise to make a corrected copy.");
        d.setDocumentDate(r.documentDate());
        d.setContent(write(clean(d.getDocType(), r.content(), false)));
        audit.log("MEDICAL_DOC_DRAFT_SAVED", "MedicalDocument", id, null);
        return view(d);
    }

    @Transactional
    public MedicalDocView issue(User actor, Long id) {
        MedicalDocument d = owned(actor, id);
        if (d.getStatus() != MedicalDocStatus.DRAFT) throw ApiException.conflict("This document is already issued.");
        Map<String, String> content = clean(d.getDocType(), read(d), true);
        Doctor doc = d.getDoctor();
        if (doc.getRegistrationNumber() == null || doc.getRegistrationNumber().isBlank())
            throw ApiException.conflict("Your medical registration number is missing. Ask the admin to add it under Admin > Doctors before issuing certificates.");
        d.setContent(write(content));
        d.setDoctorNameSnapshot(doc.getUser().getFullName());
        d.setDoctorQualificationSnapshot(doc.getQualification());
        d.setDoctorRegistrationSnapshot(doc.getRegistrationNumber());
        d.setPatientNameSnapshot(d.getPatient().getFullName());
        d.setPatientCodeSnapshot(d.getPatient().getPatientCode());
        d.setStatus(MedicalDocStatus.ISSUED);
        d.setIssuedAt(LocalDateTime.now());
        audit.log("MEDICAL_DOC_ISSUED", "MedicalDocument", id, d.getDocType().name());
        return view(d);
    }

    /** Issued documents stay untouched for the record; a correction is a new draft linked to the original. */
    @Transactional
    public MedicalDocView revise(User actor, Long id) {
        MedicalDocument src = owned(actor, id);
        if (src.getStatus() != MedicalDocStatus.ISSUED) throw ApiException.conflict("Only an issued document can be revised.");
        MedicalDocument d = new MedicalDocument();
        d.setDocCode("TMP-" + UUID.randomUUID().toString().substring(0, 12));
        d.setPatient(src.getPatient());
        d.setDoctor(src.getDoctor());
        d.setDocType(src.getDocType());
        d.setStatus(MedicalDocStatus.DRAFT);
        d.setDocumentDate(LocalDate.now());
        d.setContent(src.getContent());
        d.setRevisionOf(src);
        d = docs.saveAndFlush(d);
        d.setDocCode(Codes.of("MD", d.getId()));
        audit.log("MEDICAL_DOC_REVISION_STARTED", "MedicalDocument", d.getId(), "of " + src.getDocCode());
        return view(d);
    }

    @Transactional
    public void delete(User actor, Long id) {
        MedicalDocument d = owned(actor, id);
        if (d.getStatus() != MedicalDocStatus.DRAFT) throw ApiException.conflict("Issued documents are kept as a record and cannot be deleted.");
        docs.delete(d);
        audit.log("MEDICAL_DOC_DRAFT_DELETED", "MedicalDocument", id, null);
    }

    public MedicalDocView get(User actor, Long id) { return view(readable(actor, id)); }

    public List<MedicalDocView> list(User actor, Long patientId, MedicalDocType type, MedicalDocStatus status) {
        List<MedicalDocument> list;
        if (patientId != null) {
            access.requirePatient(actor, patientId);
            list = docs.findByPatientIdOrderByUpdatedAtDesc(patientId);
        } else {
            list = docs.findTop50ByDoctorIdOrderByUpdatedAtDesc(access.doctorOf(actor).getId());
        }
        return list.stream().filter(d -> type == null || d.getDocType() == type).filter(d -> status == null || d.getStatus() == status).map(this::view).toList();
    }

    /** Called when the doctor presses Print / downloads the PDF. Only issued documents can be printed. */
    @Transactional
    public MedicalDocView markPrinted(User actor, Long id) {
        MedicalDocument d = readable(actor, id);
        if (d.getStatus() != MedicalDocStatus.ISSUED) throw ApiException.conflict("Issue the document before printing it.");
        d.setPrintCount(d.getPrintCount() + 1);
        audit.log("MEDICAL_DOC_PRINTED", "MedicalDocument", id, "print #" + d.getPrintCount());
        return view(d);
    }

    // ---------- print area ----------

    private int mm(String key, int fallback) { return settings.getInt(key, fallback); }

    public PrintSettings printSettings() {
        return new PrintSettings(mm("print_margin_top_mm", 55), mm("print_margin_bottom_mm", 30), mm("print_margin_left_mm", 20), mm("print_margin_right_mm", 20));
    }

    @Transactional
    public PrintSettings updatePrintSettings(PrintSettings p) {
        settings.update(Map.of("print_margin_top_mm", String.valueOf(p.top()), "print_margin_bottom_mm", String.valueOf(p.bottom()),
                "print_margin_left_mm", String.valueOf(p.left()), "print_margin_right_mm", String.valueOf(p.right())));
        return printSettings();
    }
}
