package com.munot.hospital.dto;

import jakarta.validation.constraints.*;
import java.util.List;

public final class TranslateDtos {
    private TranslateDtos() {}

    public enum Target { MR, HI }

    public record TranslateRequest(@NotNull Target target, @NotEmpty @Size(max = 60) List<@Size(max = 2000) String> texts) {}

    /** method: GLOSSARY | MACHINE | PARTIAL (some sentences could not be translated and were left in English). */
    public record TranslatedText(String source, String text, String method, boolean complete) {}

    public record TranslateResponse(List<TranslatedText> translations, boolean machineAvailable) {}
}
