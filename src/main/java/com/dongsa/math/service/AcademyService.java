package com.dongsa.math.service;

import com.dongsa.math.common.ApiException;
import com.dongsa.math.common.ErrorCode;
import com.dongsa.math.domain.Academy;
import com.dongsa.math.domain.Teacher;
import com.dongsa.math.domain.TeacherRole;
import com.dongsa.math.repository.AcademyRepository;
import com.dongsa.math.repository.StudentRepository;
import com.dongsa.math.repository.TeacherRepository;
import com.dongsa.math.security.LoginUser;
import com.dongsa.math.web.dto.AcademyDtos.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class AcademyService {

    private final AcademyRepository academies;
    private final TeacherRepository teachers;
    private final StudentRepository students;
    private final PasswordEncoder encoder;

    public AcademyService(AcademyRepository academies, TeacherRepository teachers,
                          StudentRepository students, PasswordEncoder encoder) {
        this.academies = academies;
        this.teachers = teachers;
        this.students = students;
        this.encoder = encoder;
    }

    public AcademyResponse get(LoginUser me) {
        Academy a = academies.findById(me.academyId())
                .orElseThrow(() -> new ApiException(ErrorCode.ACADEMY_NOT_FOUND));
        return new AcademyResponse(a.getId(), a.getName(), a.getCode(), students.countByAcademyId(a.getId()));
    }

    @Transactional
    public AcademyResponse rename(LoginUser me, RenameRequest req) {
        requireOwner(me);
        Academy a = academies.findById(me.academyId())
                .orElseThrow(() -> new ApiException(ErrorCode.ACADEMY_NOT_FOUND));
        a.rename(req.name().trim());
        return new AcademyResponse(a.getId(), a.getName(), a.getCode(), students.countByAcademyId(a.getId()));
    }

    public List<TeacherResponse> listTeachers(LoginUser me) {
        return teachers.findByAcademyIdOrderByIdAsc(me.academyId()).stream().map(this::toDto).toList();
    }

    /** 강사는 스스로 가입하지 않는다. 원장이 계정을 만들어 주고 첫 비밀번호를 알려 준다. */
    @Transactional
    public TeacherResponse createTeacher(LoginUser me, CreateTeacherRequest req) {
        requireOwner(me);
        String email = req.email().trim().toLowerCase();
        if (teachers.existsByEmail(email)) {
            throw new ApiException(ErrorCode.EMAIL_TAKEN);
        }
        Academy academy = academies.findById(me.academyId())
                .orElseThrow(() -> new ApiException(ErrorCode.ACADEMY_NOT_FOUND));
        Teacher saved = teachers.save(new Teacher(
                academy, email, encoder.encode(req.password()), req.name().trim(), TeacherRole.TEACHER));
        return toDto(saved);
    }

    @Transactional
    public TeacherResponse setTeacherActive(LoginUser me, Long teacherId, boolean active) {
        requireOwner(me);
        if (me.id().equals(teacherId)) {
            throw new ApiException(ErrorCode.CANNOT_DISABLE_SELF);
        }
        Teacher teacher = teachers.findByIdAndAcademyId(teacherId, me.academyId())
                .orElseThrow(() -> new ApiException(ErrorCode.TEACHER_NOT_FOUND));
        teacher.setActive(active);
        return toDto(teacher);
    }

    private void requireOwner(LoginUser me) {
        if (!me.isOwner()) {
            throw new ApiException(ErrorCode.OWNER_ONLY);
        }
    }

    private TeacherResponse toDto(Teacher t) {
        return new TeacherResponse(t.getId(), t.getEmail(), t.getName(), t.getRole().name(),
                t.isActive(), t.getCreatedAt());
    }
}
