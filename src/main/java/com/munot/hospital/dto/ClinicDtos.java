package com.munot.hospital.dto;

import jakarta.validation.constraints.*;

public final class ClinicDtos {
    private ClinicDtos() {}

    public record ClinicPhotoView(Long id, String caption, int sortOrder, boolean active, long version) {}

    public record ClinicPhotoUpdate(@Size(max = 200) String caption, @Min(0) @Max(9999) int sortOrder, boolean active) {}
}
