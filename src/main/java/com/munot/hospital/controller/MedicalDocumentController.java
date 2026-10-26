package com.munot.hospital.controller;

import com.munot.hospital.dto.MedicalDocDtos.*;
import com.munot.hospital.entity.MedicalDocStatus;
import com.munot.hospital.entity.MedicalDocType;
import com.munot.hospital.pdf.MedicalDocumentPdfService;
import com.munot.hospital.security.CurrentUser;
import com.munot.hospital.service.MedicalDocumentService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/medical-documents")
@PreAuthorize("hasRole('DOCTOR')")
public class MedicalDocumentController {
    private final MedicalDocumentService docs;
    private final MedicalDocumentPdfService pdf;

    public MedicalDocumentController(MedicalDocumentService docs, MedicalDocumentPdfService pdf) { this.docs = docs; this.pdf = pdf; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MedicalDocView create(@Valid @RequestBody CreateRequest r) { return docs.create(CurrentUser.get(), r); }

    @GetMapping
    public List<MedicalDocView> list(@RequestParam(required = false) Long patientId, @RequestParam(required = false) MedicalDocType type,
                                     @RequestParam(required = false) MedicalDocStatus status) {
        return docs.list(CurrentUser.get(), patientId, type, status);
    }

    @GetMapping("/{id}")
    public MedicalDocView get(@PathVariable Long id) { return docs.get(CurrentUser.get(), id); }

    @PutMapping("/{id}")
    public MedicalDocView update(@PathVariable Long id, @Valid @RequestBody UpdateRequest r) { return docs.update(CurrentUser.get(), id, r); }

    @PostMapping("/{id}/issue")
    public MedicalDocView issue(@PathVariable Long id) { return docs.issue(CurrentUser.get(), id); }

    @PostMapping("/{id}/revise")
    @ResponseStatus(HttpStatus.CREATED)
    public MedicalDocView revise(@PathVariable Long id) { return docs.revise(CurrentUser.get(), id); }

    @PostMapping("/{id}/printed")
    public MedicalDocView printed(@PathVariable Long id) { return docs.markPrinted(CurrentUser.get(), id); }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) { docs.delete(CurrentUser.get(), id); }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable Long id) {
        byte[] data = pdf.pdf(CurrentUser.get(), id);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"medical-document-" + id + ".pdf\"").body(data);
    }

    @GetMapping("/print-settings")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    public PrintSettings printSettings() { return docs.printSettings(); }

    @PutMapping("/print-settings")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    public PrintSettings updatePrintSettings(@Valid @RequestBody PrintSettings p) { return docs.updatePrintSettings(p); }
}
