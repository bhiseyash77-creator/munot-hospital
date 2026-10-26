package com.munot.hospital.dto;

import com.munot.hospital.entity.*;
import jakarta.validation.constraints.*;

public final class ContentDtos {
    private ContentDtos() {}

    public record ContentRequest(@NotNull ContentType type, @Size(max = 120) String slug, @NotBlank @Size(max = 200) String title,
                                 @Size(max = 200) String subtitle, @Size(max = 20000) String body, @Size(max = 40) String icon,
                                 @Min(0) @Max(9999) int sortOrder, boolean active) {}

    public record ContentView(Long id, ContentType type, String slug, String title, String subtitle, String body, String icon,
                              int sortOrder, boolean active) {}
}
