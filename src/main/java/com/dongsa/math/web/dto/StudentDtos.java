package com.dongsa.math.web.dto;

import jakarta.validation.constraints.*;

public final class StudentDtos {

    private StudentDtos() {}

    public record CreateRequest(
            @NotBlank @Size(max = 30) String name,
            @NotNull @Min(1) @Max(6) Integer grade,
            @Pattern(regexp = "\\d{4}", message = "PIN은 숫자 네 자리입니다.") String pin,
            Long classroomId) {}

    public record UpdateRequest(
            @Size(max = 30) String name,
            @Min(1) @Max(6) Integer grade,
            Long classroomId) {}

    public record PinRequest(
            @Pattern(regexp = "\\d{4}", message = "PIN은 숫자 네 자리입니다.") String pin) {}

    public record ActiveRequest(boolean active) {}

    public record StudentResponse(
            Long id, String name, int grade, Long classroomId, String classroomName, boolean active) {}

    /** 학생을 만들거나 PIN 을 다시 발급했을 때만 PIN 평문을 한 번 돌려준다. 이후로는 절대 못 본다. */
    public record StudentWithPinResponse(StudentResponse student, String pin) {}

    /** 무엇이 같이 사라졌는지 알려 준다. 되돌릴 수 없으므로 숫자로 확인시켜 준다. */
    public record DeletedResponse(Long id, String name, int removedAssignments, int removedSubmissions) {}
}
