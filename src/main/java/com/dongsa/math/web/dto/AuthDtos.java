package com.dongsa.math.web.dto;

import jakarta.validation.constraints.*;

/** 로그인·회원가입에서 오가는 값들. record 라서 값이 만들어진 뒤에는 바뀌지 않는다. */
public final class AuthDtos {

    private AuthDtos() {}

    /** 고모(원장) 회원가입. 계정과 학원이 한 번에 만들어진다. */
    public record SignupRequest(
            @NotBlank @Email @Size(max = 120) String email,
            @NotBlank @Size(min = 8, max = 64) String password,
            @NotBlank @Size(max = 30) String name,
            @NotBlank @Size(max = 60) String academyName) {}

    public record LoginRequest(
            @NotBlank String email,
            @NotBlank String password) {}

    /** 학생 로그인. 이메일도 비밀번호도 없다. */
    public record StudentLoginRequest(
            @NotBlank @Size(min = 6, max = 6) String academyCode,
            @NotBlank @Size(max = 30) String name,
            @NotBlank @Pattern(regexp = "\\d{4}", message = "PIN은 숫자 네 자리입니다.") String pin) {}

    public record TokenResponse(
            String token,
            long expiresInSeconds,
            MeResponse user) {}

    public record MeResponse(
            String kind,
            Long id,
            String name,
            String role,
            Long academyId,
            String academyName,
            String academyCode,
            Integer grade) {}
}
