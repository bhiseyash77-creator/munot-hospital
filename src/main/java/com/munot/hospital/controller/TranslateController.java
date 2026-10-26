package com.munot.hospital.controller;

import com.munot.hospital.dto.TranslateDtos.*;
import com.munot.hospital.service.TranslationService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/translate")
@PreAuthorize("hasRole('DOCTOR')")
public class TranslateController {
    private final TranslationService translation;

    public TranslateController(TranslationService translation) { this.translation = translation; }

    /** English -> Marathi/Hindi for prescription wording. The doctor reviews and can edit every result. */
    @PostMapping
    public TranslateResponse translate(@Valid @RequestBody TranslateRequest req) { return translation.translate(req); }
}
