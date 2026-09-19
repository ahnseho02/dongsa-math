package com.dongsa.math.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class ClassroomDtos {

    private ClassroomDtos() {}

    public record SaveRequest(
            @NotBlank @Size(max = 40) String name,
            @Min(1) @Max(6) Integer grade) {}

    public record ClassroomResponse(Long id, String name, Integer grade, long studentCount) {}
}
