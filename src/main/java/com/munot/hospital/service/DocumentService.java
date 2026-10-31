//package com.munot.hospital.service;
//
//import com.munot.hospital.common.ApiException;
//import com.munot.hospital.config.AppProperties;
//import com.munot.hospital.dto.ClinicalDtos.ReportView;
//import com.munot.hospital.entity.*;
//import com.munot.hospital.repository.*;
//import org.springframework.core.io.PathResource;
//import org.springframework.core.io.Resource;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//import org.springframework.web.multipart.MultipartFile;
//import java.io.IOException;
//import java.io.InputStream;
//import java.nio.file.*;
//import java.time.LocalDate;
//import java.util.*;
//
//@Service
//@Transactional(readOnly = true)
//public class DocumentService {
//    private static final Map<String, String> TYPES = Map.of("pdf", "application/pdf", "png", "image/png", "jpg", "image/jpeg", "jpeg", "image/jpeg");
//
//    private final PatientDocumentRepository documents;
//    private final MedicalReportRepository reports;
//    private final AppointmentRepository appointments;
//    private final AccessControl access;
//    private final ViewMapper mapper;
//    private final AuditService audit;
//    private final Path root;
//
//    public DocumentService(PatientDocumentRepository documents, MedicalReportRepository reports, AppointmentRepository appointments,
//                           AccessControl access, ViewMapper mapper, AuditService audit, AppProperties props) {
//        this.documents = documents;
//        this.reports = reports;
//        this.appointments = appointments;
//        this.access = access;
//        this.mapper = mapper;
//        this.audit = audit;
//        this.root = Paths.get(props.uploadDir()).toAbsolutePath().normalize();
//    }
//
//    public record Download(Resource resource, String fileName, String contentType) {}
//
//    @Transactional
//    public ReportView upload(Long patientId, MultipartFile file, ReportType type, String title, String notes, LocalDate reportDate,
//                             Long appointmentId, User actor) throws IOException {
//        if (actor.getRole() == Role.PATIENT) throw ApiException.forbidden("Patients cannot upload documents");
//        if (actor.getRole() == Role.ADMIN) throw ApiException.forbidden("Access denied");
//        Patient patient = access.requirePatient(actor, patientId); // doctors only for patients they treat
//        if (file == null || file.isEmpty()) throw ApiException.badRequest("Please choose a file");
//        String original = Optional.ofNullable(file.getOriginalFilename()).orElse("document");
//        original = Paths.get(original).getFileName().toString().replaceAll("[^A-Za-z0-9._ -]", "_");
//        String ext = original.contains(".") ? original.substring(original.lastIndexOf('.') + 1).toLowerCase() : "";
//        String contentType = TYPES.get(ext);
//        if (contentType == null) throw ApiException.badRequest("Only PDF, PNG and JPG files are allowed");
//        byte[] head;
//        try (InputStream in = file.getInputStream()) { head = in.readNBytes(8); }
//        if (!matchesSignature(ext, head)) throw ApiException.badRequest("File content does not match its type");
//        Appointment appt = null;
//        if (appointmentId != null) {
//            appt = appointments.findById(appointmentId).orElseThrow(() -> ApiException.notFound("Appointment not found"));
//            if (!appt.getPatient().getId().equals(patientId)) throw ApiException.badRequest("Appointment does not belong to this patient");
//        }
//        String stored = UUID.randomUUID() + "." + ext;
//        Path dir = root.resolve(String.valueOf(patientId)).normalize();
//        Files.createDirectories(dir);
//        Path target = dir.resolve(stored).normalize();
//        if (!target.startsWith(root)) throw ApiException.badRequest("Invalid file path");
//        try (InputStream in = file.getInputStream()) { Files.copy(in, target); }
//        try {
//            PatientDocument d = new PatientDocument();
//            d.setPatient(patient);
//            d.setFileName(original);
//            d.setStoredName(stored);
//            d.setContentType(contentType);
//            d.setSizeBytes(file.getSize());
//            d.setUploadedBy(actor);
//            d = documents.save(d);
//            MedicalReport r = new MedicalReport();
//            r.setPatient(patient);
//            r.setAppointment(appt);
//            r.setDocument(d);
//            r.setReportType(type == null ? ReportType.OTHER : type);
//            r.setTitle(title == null || title.isBlank() ? original : title.trim());
//            r.setNotes(notes);
//            r.setReportDate(reportDate == null ? LocalDate.now() : reportDate);
//            r = reports.save(r);
//            audit.log("DOCUMENT_UPLOADED", "PatientDocument", d.getId(), r.getReportType() + " for patient " + patientId);
//            return mapper.report(r);
//        } catch (RuntimeException e) {
//            Files.deleteIfExists(target);
//            throw e;
//        }
//    }
//
//    private static boolean matchesSignature(String ext, byte[] h) {
//        if (h.length < 4) return false;
//        return switch (ext) {
//            case "pdf" -> h[0] == '%' && h[1] == 'P' && h[2] == 'D' && h[3] == 'F';
//            case "png" -> (h[0] & 0xFF) == 0x89 && h[1] == 'P' && h[2] == 'N' && h[3] == 'G';
//            default -> (h[0] & 0xFF) == 0xFF && (h[1] & 0xFF) == 0xD8 && (h[2] & 0xFF) == 0xFF;
//        };
//    }
//
//    public List<ReportView> forPatient(Long patientId, User actor) {
//        if (actor.getRole() == Role.ADMIN) throw ApiException.forbidden("Access denied");
//        access.requirePatient(actor, patientId);
//        return reports.findByPatientIdOrderByReportDateDescIdDesc(patientId).stream().map(mapper::report).toList();
//    }
//
//    public List<ReportView> mine(User actor) {
//        Patient p = access.patientOf(actor);
//        return reports.findByPatientIdOrderByReportDateDescIdDesc(p.getId()).stream().map(mapper::report).toList();
//    }
//
//    public Download download(Long documentId, User actor) {
//        if (actor.getRole() == Role.ADMIN) throw ApiException.forbidden("Access denied");
//        PatientDocument d = documents.findById(documentId).orElseThrow(() -> ApiException.notFound("Document not found"));
//        access.requirePatient(actor, d.getPatient().getId());
//        Path file = root.resolve(String.valueOf(d.getPatient().getId())).resolve(d.getStoredName()).normalize();
//        if (!file.startsWith(root) || !Files.isReadable(file)) throw ApiException.notFound("File not found on server");
//        audit.log("DOCUMENT_DOWNLOADED", "PatientDocument", d.getId(), null);
//        return new Download(new PathResource(file), d.getFileName(), d.getContentType());
//    }
//}
package com.munot.hospital.service;

import com.munot.hospital.common.ApiException;
import com.munot.hospital.config.AppProperties;
import com.munot.hospital.dto.ClinicalDtos.ReportView;
import com.munot.hospital.entity.*;
import com.munot.hospital.repository.*;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class DocumentService {
    private static final Map<String, String> TYPES = Map.of("pdf", "application/pdf", "png", "image/png", "jpg", "image/jpeg", "jpeg", "image/jpeg");

    private final PatientDocumentRepository documents;
    private final MedicalReportRepository reports;
    private final AppointmentRepository appointments;
    private final AccessControl access;
    private final ViewMapper mapper;
    private final AuditService audit;
    private final Path root;

    public DocumentService(PatientDocumentRepository documents, MedicalReportRepository reports, AppointmentRepository appointments,
                           AccessControl access, ViewMapper mapper, AuditService audit, AppProperties props) {
        this.documents = documents;
        this.reports = reports;
        this.appointments = appointments;
        this.access = access;
        this.mapper = mapper;
        this.audit = audit;
        this.root = Paths.get(props.uploadDir()).toAbsolutePath().normalize();
    }

    public record Download(Resource resource, String fileName, String contentType) {}

    @Transactional
    public ReportView upload(Long patientId, MultipartFile file, ReportType type, String title, String notes, LocalDate reportDate,
                             Long appointmentId, User actor) throws IOException {
        if (actor.getRole() == Role.PATIENT) throw ApiException.forbidden("Patients cannot upload documents");
        if (actor.getRole() == Role.ADMIN) throw ApiException.forbidden("Access denied");
        Patient patient = access.requirePatientRecord(actor, patientId);
        if (file == null || file.isEmpty()) throw ApiException.badRequest("Please choose a file");
        String original = Optional.ofNullable(file.getOriginalFilename()).orElse("document");
        original = Paths.get(original).getFileName().toString().replaceAll("[^A-Za-z0-9._ -]", "_");
        String ext = original.contains(".") ? original.substring(original.lastIndexOf('.') + 1).toLowerCase() : "";
        String contentType = TYPES.get(ext);
        if (contentType == null) throw ApiException.badRequest("Only PDF, PNG and JPG files are allowed");
        byte[] head;
        try (InputStream in = file.getInputStream()) { head = in.readNBytes(8); }
        if (!matchesSignature(ext, head)) throw ApiException.badRequest("File content does not match its type");
        Appointment appt = null;
        if (appointmentId != null) {
            appt = appointments.findById(appointmentId).orElseThrow(() -> ApiException.notFound("Appointment not found"));
            if (!appt.getPatient().getId().equals(patientId)) throw ApiException.badRequest("Appointment does not belong to this patient");
        }
        String stored = UUID.randomUUID() + "." + ext;
        Path dir = root.resolve(String.valueOf(patientId)).normalize();
        Files.createDirectories(dir);
        Path target = dir.resolve(stored).normalize();
        if (!target.startsWith(root)) throw ApiException.badRequest("Invalid file path");
        try (InputStream in = file.getInputStream()) { Files.copy(in, target); }
        try {
            PatientDocument d = new PatientDocument();
            d.setPatient(patient);
            d.setFileName(original);
            d.setStoredName(stored);
            d.setContentType(contentType);
            d.setSizeBytes(file.getSize());
            d.setUploadedBy(actor);
            d = documents.save(d);
            MedicalReport r = new MedicalReport();
            r.setPatient(patient);
            r.setAppointment(appt);
            r.setDocument(d);
            r.setReportType(type == null ? ReportType.OTHER : type);
            r.setTitle(title == null || title.isBlank() ? original : title.trim());
            r.setNotes(notes);
            r.setReportDate(reportDate == null ? LocalDate.now() : reportDate);
            r = reports.save(r);
            audit.log("DOCUMENT_UPLOADED", "PatientDocument", d.getId(), r.getReportType() + " for patient " + patientId);
            return mapper.report(r);
        } catch (RuntimeException e) {
            Files.deleteIfExists(target);
            throw e;
        }
    }

    private static boolean matchesSignature(String ext, byte[] h) {
        if (h.length < 4) return false;
        return switch (ext) {
            case "pdf" -> h[0] == '%' && h[1] == 'P' && h[2] == 'D' && h[3] == 'F';
            case "png" -> (h[0] & 0xFF) == 0x89 && h[1] == 'P' && h[2] == 'N' && h[3] == 'G';
            default -> (h[0] & 0xFF) == 0xFF && (h[1] & 0xFF) == 0xD8 && (h[2] & 0xFF) == 0xFF;
        };
    }

    public List<ReportView> forPatient(Long patientId, User actor) {
        if (actor.getRole() == Role.ADMIN) throw ApiException.forbidden("Access denied");
        access.requirePatientRecord(actor, patientId);
        return reports.findByPatientIdOrderByReportDateDescIdDesc(patientId).stream().map(mapper::report).toList();
    }

    public List<ReportView> mine(User actor) {
        Patient p = access.patientOf(actor);
        return reports.findByPatientIdOrderByReportDateDescIdDesc(p.getId()).stream().map(mapper::report).toList();
    }

    public Download download(Long documentId, User actor) {
        if (actor.getRole() == Role.ADMIN) throw ApiException.forbidden("Access denied");
        PatientDocument d = documents.findById(documentId).orElseThrow(() -> ApiException.notFound("Document not found"));
        access.requirePatientRecord(actor, d.getPatient().getId());
        Path file = root.resolve(String.valueOf(d.getPatient().getId())).resolve(d.getStoredName()).normalize();
        if (!file.startsWith(root) || !Files.isReadable(file)) throw ApiException.notFound("File not found on server");
        audit.log("DOCUMENT_DOWNLOADED", "PatientDocument", d.getId(), null);
        return new Download(new PathResource(file), d.getFileName(), d.getContentType());
    }
}
