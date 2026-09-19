package com.dongsa.math.service;

import com.dongsa.math.common.ApiException;
import com.dongsa.math.common.ErrorCode;
import com.dongsa.math.domain.Academy;
import com.dongsa.math.domain.Classroom;
import com.dongsa.math.domain.Student;
import com.dongsa.math.repository.AcademyRepository;
import com.dongsa.math.repository.GameRoundRepository;
import com.dongsa.math.repository.StudentAssignmentRepository;
import com.dongsa.math.repository.StudentRepository;
import com.dongsa.math.repository.SubmissionRepository;
import com.dongsa.math.security.LoginUser;
import com.dongsa.math.web.dto.StudentDtos.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class StudentService {

    private final StudentRepository students;
    private final AcademyRepository academies;
    private final ClassroomService classroomService;
    private final StudentAssignmentRepository studentAssignments;
    private final SubmissionRepository submissions;
    private final GameRoundRepository gameRounds;
    private final PasswordEncoder encoder;
    private final SecureRandom random = new SecureRandom();

    public StudentService(StudentRepository students, AcademyRepository academies,
                          ClassroomService classroomService, StudentAssignmentRepository studentAssignments,
                          SubmissionRepository submissions, GameRoundRepository gameRounds,
                          PasswordEncoder encoder) {
        this.students = students;
        this.academies = academies;
        this.classroomService = classroomService;
        this.studentAssignments = studentAssignments;
        this.submissions = submissions;
        this.gameRounds = gameRounds;
        this.encoder = encoder;
    }

    public List<StudentResponse> list(LoginUser me, Long classroomId) {
        List<Student> found = (classroomId == null)
                ? students.findByAcademyIdOrderByNameAsc(me.academyId())
                : students.findByAcademyIdAndClassroomIdOrderByNameAsc(me.academyId(), classroomId);
        return found.stream().map(this::toDto).toList();
    }

    /**
     * 학생 등록. PIN 을 비워 두면 자동으로 만들어 준다.
     * 만들어진 PIN 은 이 응답에서 딱 한 번만 보인다 — 저장은 해시로만 하기 때문이다.
     */
    @Transactional
    public StudentWithPinResponse create(LoginUser me, CreateRequest req) {
        String pin = (req.pin() == null || req.pin().isBlank()) ? randomPin() : req.pin();
        String name = req.name().trim();
        requirePinIsDistinguishable(me.academyId(), name, pin, null);

        Academy academy = academies.findById(me.academyId())
                .orElseThrow(() -> new ApiException(ErrorCode.ACADEMY_NOT_FOUND));
        Classroom classroom = (req.classroomId() == null) ? null : classroomService.mine(me, req.classroomId());

        Student saved = students.save(new Student(academy, classroom, name, encoder.encode(pin), req.grade()));
        return new StudentWithPinResponse(toDto(saved), pin);
    }

    @Transactional
    public StudentResponse update(LoginUser me, Long id, UpdateRequest req) {
        Student student = mine(me, id);
        Classroom classroom = (req.classroomId() == null) ? null : classroomService.mine(me, req.classroomId());
        student.update(req.name() == null ? null : req.name().trim(), req.grade(), classroom);
        return toDto(student);
    }

    /** PIN 을 잊어버렸을 때. 선생님이 새로 발급하고 아이에게 알려 준다. */
    @Transactional
    public StudentWithPinResponse resetPin(LoginUser me, Long id, PinRequest req) {
        Student student = mine(me, id);
        String pin = (req == null || req.pin() == null || req.pin().isBlank()) ? randomPin() : req.pin();
        requirePinIsDistinguishable(me.academyId(), student.getName(), pin, student.getId());
        student.changePin(encoder.encode(pin));
        return new StudentWithPinResponse(toDto(student), pin);
    }

    @Transactional
    public StudentResponse setActive(LoginUser me, Long id, boolean active) {
        Student student = mine(me, id);
        student.setActive(active);
        return toDto(student);
    }

    /**
     * 학생을 완전히 지운다. 과제 기록과 성적도 같이 사라지고 되돌릴 수 없다.
     *
     * 평소에는 중지(active=false)를 쓴다 — 그만둔 아이의 기록은 남겨 두는 편이 낫다.
     * 완전 삭제는 개인정보를 지워 달라는 요청처럼 정말 없애야 할 때를 위한 것이라
     * 원장만 할 수 있다.
     */
    @Transactional
    public DeletedStudent delete(LoginUser me, Long id) {
        if (!me.isOwner()) {
            throw new ApiException(ErrorCode.OWNER_ONLY);
        }
        Student student = mine(me, id);
        String name = student.getName();

        // 외래키를 제한(restrict)으로 두었으므로 기록부터 순서대로 지운다.
        // 제약을 느슨하게 풀어 두면 실수로 지울 때 성적이 조용히 날아간다.
        int removedSubmissions = submissions.deleteAllForStudent(id);
        int removedAssignments = studentAssignments.deleteAllForStudent(id);
        gameRounds.deleteAllForStudent(id);
        students.delete(student);

        return new DeletedStudent(id, name, removedAssignments, removedSubmissions);
    }

    public record DeletedStudent(Long id, String name, int assignments, int submissions) {}

    // ── 내부 ──

    private Student mine(LoginUser me, Long id) {
        return students.findByIdAndAcademyId(id, me.academyId())
                .orElseThrow(() -> new ApiException(ErrorCode.STUDENT_NOT_FOUND));
    }

    /**
     * 학생은 이름 + PIN 으로 로그인한다. 같은 학원에 이름이 같은 아이가 둘 있고 PIN 까지 같으면
     * 누구로 로그인되는지 정해지지 않는다. 그래서 등록 단계에서 막는다.
     */
    private void requirePinIsDistinguishable(Long academyId, String name, String pin, Long selfId) {
        boolean clash = students.findByAcademyIdAndNameAndActiveTrue(academyId, name).stream()
                .filter(s -> !s.getId().equals(selfId))
                .anyMatch(s -> encoder.matches(pin, s.getPinHash()));
        if (clash) {
            throw new ApiException(ErrorCode.DUPLICATE_STUDENT_PIN);
        }
    }

    private String randomPin() {
        return String.format("%04d", random.nextInt(10000));
    }

    private StudentResponse toDto(Student s) {
        Classroom c = s.getClassroom();
        return new StudentResponse(s.getId(), s.getName(), s.getGrade(),
                c == null ? null : c.getId(), c == null ? null : c.getName(), s.isActive());
    }
}
