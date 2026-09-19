package com.dongsa.math.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class AcademyDtos {

    private AcademyDtos() {}

    public record AcademyResponse(Long id, String name, String code, long studentCount) {}

    public record RenameRequest(@NotBlank @Size(max = 60) String name) {}

    /** 원장이 강사 계정을 만들어 준다. 강사가 스스로 가입하지 않는다. */
    public record CreateTeacherRequest(
            @NotBlank @Email @Size(max = 120) String email,
            @NotBlank @Size(min = 8, max = 64) String password,
            @NotBlank @Size(max = 30) String name) {}

    public record TeacherResponse(Long id, String email, String name, String role, boolean active, Instant createdAt) {}

    public record ActiveRequest(boolean active) {}
}
