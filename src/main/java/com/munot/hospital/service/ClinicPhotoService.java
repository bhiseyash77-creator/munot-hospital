package com.munot.hospital.service;

import com.munot.hospital.common.ApiException;
import com.munot.hospital.config.AppProperties;
import com.munot.hospital.dto.ClinicDtos.*;
import com.munot.hospital.entity.ClinicPhoto;
import com.munot.hospital.repository.ClinicPhotoRepository;
import com.munot.hospital.util.ImageRules;
import org.springframework.core.io.PathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.List;
import java.util.UUID;

/** Clinic photo gallery: JPG/PNG/WebP up to 5 MB each, at most 40 photos, stored under UPLOAD_DIR/clinic-photos. */
@Service
@Transactional(readOnly = true)
public class ClinicPhotoService {
    private static final long MAX_BYTES = 5L * 1024 * 1024;
    private static final int MAX_PHOTOS = 40;

    private final ClinicPhotoRepository repo;
    private final AuditService audit;
    private final Path dir;

    public ClinicPhotoService(ClinicPhotoRepository repo, AuditService audit, AppProperties props) {
        this.repo = repo;
        this.audit = audit;
        this.dir = Paths.get(props.uploadDir()).toAbsolutePath().normalize().resolve("clinic-photos");
    }

    private ClinicPhotoView view(ClinicPhoto p) {
        return new ClinicPhotoView(p.getId(), p.getCaption(), p.getSortOrder(), p.isActive(),
                p.getUpdatedAt() == null ? 0L : p.getUpdatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());
    }

    public List<ClinicPhotoView> publicList() { return repo.findByActiveTrueOrderBySortOrderAscIdAsc().stream().map(this::view).toList(); }

    public List<ClinicPhotoView> adminList() { return repo.findAllByOrderBySortOrderAscIdAsc().stream().map(this::view).toList(); }

    @Transactional
    public ClinicPhotoView upload(MultipartFile file, String caption, int sortOrder) throws IOException {
        if (file == null || file.isEmpty()) throw ApiException.badRequest("Please choose a photo");
        if (file.getSize() > MAX_BYTES) throw ApiException.badRequest("Each photo must be 5 MB or smaller");
        if (repo.count() >= MAX_PHOTOS) throw ApiException.conflict("The gallery is full (" + MAX_PHOTOS + " photos). Delete one first.");
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
        try {
            ClinicPhoto p = new ClinicPhoto();
            p.setCaption(caption == null || caption.isBlank() ? null : caption.trim().substring(0, Math.min(caption.trim().length(), 200)));
            p.setStoredName(stored);
            p.setContentType(type);
            p.setSortOrder(Math.max(0, Math.min(sortOrder, 9999)));
            p = repo.save(p);
            audit.log("CLINIC_PHOTO_ADDED", "ClinicPhoto", p.getId(), p.getCaption());
            return view(p);
        } catch (RuntimeException e) {
            Files.deleteIfExists(target);
            throw e;
        }
    }

    @Transactional
    public ClinicPhotoView update(Long id, ClinicPhotoUpdate u) {
        ClinicPhoto p = repo.findById(id).orElseThrow(() -> ApiException.notFound("Photo not found"));
        p.setCaption(u.caption() == null || u.caption().isBlank() ? null : u.caption().trim());
        p.setSortOrder(u.sortOrder());
        p.setActive(u.active());
        audit.log("CLINIC_PHOTO_UPDATED", "ClinicPhoto", id, null);
        return view(repo.save(p));
    }

    @Transactional
    public void delete(Long id) throws IOException {
        ClinicPhoto p = repo.findById(id).orElseThrow(() -> ApiException.notFound("Photo not found"));
        repo.delete(p);
        Files.deleteIfExists(dir.resolve(p.getStoredName()).normalize());
        audit.log("CLINIC_PHOTO_DELETED", "ClinicPhoto", id, null);
    }

    /** publicOnly=true serves only photos that are shown on the website. */
    public DocumentService.Download load(Long id, boolean publicOnly) {
        ClinicPhoto p = repo.findById(id).orElseThrow(() -> ApiException.notFound("Photo not found"));
        if (publicOnly && !p.isActive()) throw ApiException.notFound("Photo not found");
        Path file = dir.resolve(p.getStoredName()).normalize();
        if (!file.startsWith(dir) || !Files.isReadable(file)) throw ApiException.notFound("Photo not found");
        return new DocumentService.Download(new PathResource(file), p.getStoredName(), p.getContentType());
    }
}
