package com.munot.hospital.service;

import com.munot.hospital.common.ApiException;
import com.munot.hospital.config.AppProperties;
import com.munot.hospital.entity.Doctor;
import com.munot.hospital.repository.DoctorRepository;
import com.munot.hospital.util.ImageRules;
import org.springframework.core.io.PathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.UUID;

/** Doctor profile photo: validated upload (JPG/PNG/WebP, max 3 MB) stored under UPLOAD_DIR/doctor-photos. */
@Service
@Transactional(readOnly = true)
public class DoctorPhotoService {
    private static final long MAX_BYTES = 3L * 1024 * 1024;

    private final DoctorRepository doctors;
    private final AuditService audit;
    private final Path dir;

    public DoctorPhotoService(DoctorRepository doctors, AuditService audit, AppProperties props) {
        this.doctors = doctors;
        this.audit = audit;
        this.dir = Paths.get(props.uploadDir()).toAbsolutePath().normalize().resolve("doctor-photos");
    }

    @Transactional
    public void upload(Long doctorId, MultipartFile file) throws IOException {
        Doctor d = doctors.findById(doctorId).orElseThrow(() -> ApiException.notFound("Doctor not found"));
        if (file == null || file.isEmpty()) throw ApiException.badRequest("Please choose an image");
        if (file.getSize() > MAX_BYTES) throw ApiException.badRequest("Photo must be 3 MB or smaller");
        String ext = ImageRules.extension(file.getOriginalFilename());
        String type = ImageRules.contentTypeFor(ext);
        if (type == null) throw ApiException.badRequest("Only JPG, PNG or WebP images are allowed");
        byte[] head;
        try (InputStream in = file.getInputStream()) { head = in.readNBytes(12); }
        if (!ImageRules.signatureOk(ext, head)) throw ApiException.badRequest("File content does not match its type");
        Files.createDirectories(dir);
        String stored = UUID.randomUUID() + "." + ext;
        Path target = dir.resolve(stored).normalize();
        if (!target.startsWith(dir)) throw ApiException.badRequest("Invalid file path");
        try (InputStream in = file.getInputStream()) { Files.copy(in, target); }
        String old = d.getPhotoStoredName();
        d.setPhotoStoredName(stored);
        d.setPhotoContentType(type);
        doctors.save(d);
        if (old != null) Files.deleteIfExists(dir.resolve(old).normalize());
        audit.log("DOCTOR_PHOTO_UPDATED", "Doctor", doctorId, null);
    }

    @Transactional
    public void remove(Long doctorId) throws IOException {
        Doctor d = doctors.findById(doctorId).orElseThrow(() -> ApiException.notFound("Doctor not found"));
        String old = d.getPhotoStoredName();
        d.setPhotoStoredName(null);
        d.setPhotoContentType(null);
        doctors.save(d);
        if (old != null) Files.deleteIfExists(dir.resolve(old).normalize());
        audit.log("DOCTOR_PHOTO_REMOVED", "Doctor", doctorId, null);
    }

    /** Public: only active doctors' photos are served. */
    public DocumentService.Download load(Long doctorId) {
        Doctor d = doctors.findById(doctorId).orElseThrow(() -> ApiException.notFound("Photo not found"));
        if (!d.isActive() || d.getPhotoStoredName() == null) throw ApiException.notFound("Photo not found");
        Path file = dir.resolve(d.getPhotoStoredName()).normalize();
        if (!file.startsWith(dir) || !Files.isReadable(file)) throw ApiException.notFound("Photo not found");
        return new DocumentService.Download(new PathResource(file), d.getPhotoStoredName(), d.getPhotoContentType());
    }
}
