package com.dongsa.math.service;

import com.dongsa.math.common.ApiException;
import com.dongsa.math.common.ErrorCode;
import com.dongsa.math.domain.*;
import com.dongsa.math.repository.*;
import com.dongsa.math.security.JwtProvider;
import com.dongsa.math.security.LoginAttemptLimiter;
import com.dongsa.math.security.LoginUser;
import com.dongsa.math.web.dto.AuthDtos.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class AuthService {

    private final AcademyRepository academies;
    private final TeacherRepository teachers;
    private final StudentRepository students;
    private final AcademyCodeGenerator codes;
    private final PasswordEncoder encoder;
    private final JwtProvider jwt;
    private final LoginAttemptLimiter limiter;

    public AuthService(AcademyRepository academies, TeacherRepository teachers, StudentRepository students,
                       AcademyCodeGenerator codes, PasswordEncoder encoder, JwtProvider jwt,
                       LoginAttemptLimiter limiter) {
        this.academies = academies;
        this.teachers = teachers;
        this.students = students;
        this.codes = codes;
        this.encoder = encoder;
        this.jwt = jwt;
        this.limiter = limiter;
    }

    /**
     * 원장 회원가입. 계정 하나와 학원 하나가 같이 만들어지고 학원 코드가 발급된다.
     * 이 서비스에서 학원이 생기는 유일한 경로다.
     */
    @Transactional
    public TokenResponse signup(SignupRequest req) {
        String email = normalizeEmail(req.email());
        if (teachers.existsByEmail(email)) {
            throw new ApiException(ErrorCode.EMAIL_TAKEN);
        }
        Academy academy = academies.save(new Academy(req.academyName().trim(), codes.generate()));
        Teacher owner = teachers.save(new Teacher(
                academy, email, encoder.encode(req.password()), req.name().trim(), TeacherRole.OWNER));
        return token(toLoginUser(owner), me(owner));
    }

    public TokenResponse login(LoginRequest req, String ip) {
        String email = normalizeEmail(req.email());
        String key = LoginAttemptLimiter.teacherKey(email);
        limiter.checkAllowed(key, ip);

        Teacher teacher = teachers.findByEmail(email).orElse(null);
        if (teacher == null || !encoder.matches(req.password(), teacher.getPasswordHash())) {
            limiter.recordFailure(key, ip);
            throw new ApiException(ErrorCode.BAD_CREDENTIALS);
        }
        if (!teacher.isActive()) {
            throw new ApiException(ErrorCode.ACCOUNT_DISABLED);
        }
        limiter.recordSuccess(key);
        return token(toLoginUser(teacher), me(teacher));
    }

    /**
     * 학생 로그인. 학원 코드로 학원을 찾고, 그 안에서 이름이 같은 학생들을 PIN 으로 가린다.
     * 어느 단계에서 틀렸는지는 알려 주지 않는다 (코드만 알면 이름을 훑을 수 있으므로).
     */
    public TokenResponse studentLogin(StudentLoginRequest req, String ip) {
        String key = LoginAttemptLimiter.studentKey(req.academyCode(), req.name());
        limiter.checkAllowed(key, ip);

        Academy academy = academies.findByCode(req.academyCode().trim().toUpperCase()).orElse(null);
        Student matched = academy == null ? null
                : students.findByAcademyIdAndNameAndActiveTrue(academy.getId(), req.name().trim()).stream()
                        .filter(s -> encoder.matches(req.pin(), s.getPinHash()))
                        .findFirst()
                        .orElse(null);

        if (matched == null) {
            limiter.recordFailure(key, ip);
            throw new ApiException(ErrorCode.BAD_STUDENT_LOGIN);
        }
        limiter.recordSuccess(key);
        return token(toLoginUser(matched), me(matched, academy));
    }

    public MeResponse me(LoginUser user) {
        if (user.isTeacher()) {
            Teacher teacher = teachers.findById(user.id())
                    .orElseThrow(() -> new ApiException(ErrorCode.TEACHER_NOT_FOUND));
            return me(teacher);
        }
        Student student = students.findById(user.id())
                .orElseThrow(() -> new ApiException(ErrorCode.STUDENT_NOT_FOUND));
        return me(student, student.getAcademy());
    }

    // ── 내부 ──

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    private TokenResponse token(LoginUser user, MeResponse me) {
        long ttl = user.isTeacher() ? jwt.teacherTtlSeconds() : jwt.studentTtlSeconds();
        return new TokenResponse(jwt.issue(user), ttl, me);
    }

    private LoginUser toLoginUser(Teacher t) {
        return new LoginUser(LoginUser.Kind.TEACHER, t.getId(), t.getAcademy().getId(), t.getName(), t.getRole());
    }

    private LoginUser toLoginUser(Student s) {
        return new LoginUser(LoginUser.Kind.STUDENT, s.getId(), s.getAcademy().getId(), s.getName(), null);
    }

    private MeResponse me(Teacher t) {
        Academy a = t.getAcademy();
        return new MeResponse("TEACHER", t.getId(), t.getName(), t.getRole().name(),
                a.getId(), a.getName(), a.getCode(), null);
    }

    private MeResponse me(Student s, Academy a) {
        return new MeResponse("STUDENT", s.getId(), s.getName(), null,
                a.getId(), a.getName(), null, s.getGrade());
    }
}
