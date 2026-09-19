package com.dongsa.math.service;

import com.dongsa.math.common.ApiException;
import com.dongsa.math.common.ErrorCode;
import com.dongsa.math.domain.*;
import com.dongsa.math.problem.*;
import com.dongsa.math.repository.*;
import com.dongsa.math.security.LoginUser;
import com.dongsa.math.web.dto.AssignmentDtos.*;
import com.dongsa.math.web.dto.ReportDtos;
import com.dongsa.math.web.dto.ReportDtos.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class AssignmentService {

    private final AssignmentRepository assignments;
    private final StudentAssignmentRepository studentAssignments;
    private final SubmissionRepository submissions;
    private final StudentRepository students;
    private final ClassroomRepository classrooms;
    private final AcademyRepository academies;
    private final TeacherRepository teachers;
    private final WorksheetFactory worksheets;
    private final SecureRandom random = new SecureRandom();

    public AssignmentService(AssignmentRepository assignments, StudentAssignmentRepository studentAssignments,
                             SubmissionRepository submissions, StudentRepository students,
                             ClassroomRepository classrooms, AcademyRepository academies,
                             TeacherRepository teachers, WorksheetFactory worksheets) {
        this.assignments = assignments;
        this.studentAssignments = studentAssignments;
        this.submissions = submissions;
        this.students = students;
        this.classrooms = classrooms;
        this.academies = academies;
        this.teachers = teachers;
        this.worksheets = worksheets;
    }

    @Transactional
    public AssignmentDetail create(LoginUser me, CreateRequest req) {
        Academy academy = academies.findById(me.academyId())
                .orElseThrow(() -> new ApiException(ErrorCode.ACADEMY_NOT_FOUND));
        Teacher teacher = teachers.findByIdAndAcademyId(me.id(), me.academyId())
                .orElseThrow(() -> new ApiException(ErrorCode.TEACHER_NOT_FOUND));

        Classroom classroom = req.classroomId() == null ? null
                : classrooms.findByIdAndAcademyId(req.classroomId(), me.academyId())
                        .orElseThrow(() -> new ApiException(ErrorCode.CLASSROOM_NOT_FOUND));

        List<Student> targets = resolveTargets(me, classroom, req.studentIds());
        if (targets.isEmpty()) {
            throw new ApiException(ErrorCode.NO_TARGET_STUDENT);
        }

        Long fromCode = ReadableCode.decode(req.code());
        long seed = fromCode != null ? fromCode : Math.floorMod(random.nextLong(), ReadableCode.MAX_SEED);
        WorksheetSpec spec = new WorksheetSpec(
                req.grade(), req.categories(), req.countOrDefault(), req.carryOrDefault(), seed);

        // 문제를 실제로 만들 수 있는 조건인지 여기서 확인한다.
        // 아이가 과제를 열었을 때 터지면 늦다.
        worksheets.build(spec);

        Assignment assignment = assignments.save(new Assignment(
                academy, classroom, teacher, req.title().strip(), spec,
                req.sameForEveryoneOrDefault(), req.dueAt()));

        for (Student student : targets) {
            studentAssignments.save(new StudentAssignment(assignment, student));
        }
        return detail(me, assignment.getId());
    }

    public List<AssignmentSummary> list(LoginUser me) {
        return assignments.findByAcademyIdOrderByIdDesc(me.academyId()).stream()
                .map(this::summary)
                .toList();
    }

    public AssignmentDetail detail(LoginUser me, Long assignmentId) {
        Assignment assignment = mine(me, assignmentId);
        List<StudentAssignment> rows = studentAssignments.findWithStudentByAssignmentId(assignmentId);

        Map<Long, long[]> counts = progressByStudentAssignment(assignmentId);
        int total = assignment.totalSteps();

        List<StudentProgress> progress = rows.stream().map(sa -> {
            long[] c = counts.getOrDefault(sa.getId(), new long[]{0, 0});
            return new StudentProgress(sa.getId(), sa.getStudent().getId(), sa.getStudent().getName(),
                    sa.status(), c[0], c[1], total, percent(c[0], total));
        }).toList();

        return new AssignmentDetail(summary(assignment), progress);
    }

    @Transactional
    public AssignmentSummary setClosed(LoginUser me, Long assignmentId, boolean closed) {
        Assignment assignment = mine(me, assignmentId);
        if (closed) {
            assignment.close(Instant.now());
        } else {
            assignment.reopen();
        }
        return summary(assignment);
    }

    /** 과제 리포트 — 단계별·유형별·학생별을 쿼리 세 번으로 뽑는다. */
    public AssignmentReport report(LoginUser me, Long assignmentId) {
        Assignment assignment = mine(me, assignmentId);

        List<Rate> bySteps = new ArrayList<>();
        Map<SolveStep, long[]> stepCounts = toMap(submissions.countByStepForAssignment(assignmentId));
        for (SolveStep step : SolveStep.values()) {
            long[] c = stepCounts.getOrDefault(step, new long[]{0, 0});
            bySteps.add(Rate.of(step.name(), step.label(), c[0], c[1]));
        }

        List<Rate> byCategories = new ArrayList<>();
        Map<ProblemCategory, long[]> categoryCounts = toMap(submissions.countByCategoryForAssignment(assignmentId));
        categoryCounts.forEach((category, c) ->
                byCategories.add(Rate.of(category.name(), category.label(), c[0], c[1])));
        byCategories.sort(Comparator.comparingInt(Rate::percent));

        List<StudentRow> studentRows = studentRows(assignmentId, assignment);

        Rate weakest = bySteps.stream()
                .filter(r -> r.total() > 0)
                .min(Comparator.comparingInt(Rate::percent))
                .orElse(null);

        return new AssignmentReport(
                assignment.getId(), assignment.getTitle(),
                studentAssignments.countByAssignmentId(assignmentId),
                studentAssignments.countByAssignmentIdAndCompletedAtIsNotNull(assignmentId),
                bySteps, byCategories, studentRows,
                weakest == null ? null : weakest.key(),
                adviceFor(weakest));
    }

    // ── 내부 ──

    private List<Student> resolveTargets(LoginUser me, Classroom classroom, List<Long> studentIds) {
        Map<Long, Student> byId = new LinkedHashMap<>();
        if (classroom != null) {
            students.findByAcademyIdAndClassroomIdOrderByNameAsc(me.academyId(), classroom.getId())
                    .forEach(s -> byId.put(s.getId(), s));
        }
        if (studentIds != null) {
            for (Long id : studentIds) {
                Student student = students.findByIdAndAcademyId(id, me.academyId())
                        .orElseThrow(() -> new ApiException(ErrorCode.STUDENT_NOT_FOUND));
                byId.put(student.getId(), student);
            }
        }
        // 그만둔 아이에게는 새 과제를 내지 않는다
        return byId.values().stream().filter(Student::isActive).toList();
    }

    private Assignment mine(LoginUser me, Long assignmentId) {
        return assignments.findByIdAndAcademyId(assignmentId, me.academyId())
                .orElseThrow(() -> new ApiException(ErrorCode.ASSIGNMENT_NOT_FOUND));
    }

    private AssignmentSummary summary(Assignment a) {
        Classroom classroom = a.getClassroom();
        return new AssignmentSummary(
                a.getId(), a.getTitle(), a.getGrade(), a.getProblemCount(), a.code(), a.isSameForEveryone(),
                classroom == null ? null : classroom.getId(),
                classroom == null ? null : classroom.getName(),
                studentAssignments.countByAssignmentId(a.getId()),
                studentAssignments.countByAssignmentIdAndCompletedAtIsNotNull(a.getId()),
                a.getDueAt(), a.isClosed(), a.getCreatedAt());
    }

    /** studentAssignmentId → {맞힌 수, 낸 수} */
    private Map<Long, long[]> progressByStudentAssignment(Long assignmentId) {
        Map<Long, long[]> result = new HashMap<>();
        for (Object[] row : submissions.countByStudentAssignmentAndStep(assignmentId)) {
            long[] c = result.computeIfAbsent((Long) row[0], k -> new long[]{0, 0});
            c[0] += ((Number) row[2]).longValue();
            c[1] += ((Number) row[3]).longValue();
        }
        return result;
    }

    private List<StudentRow> studentRows(Long assignmentId, Assignment assignment) {
        Map<Long, Map<SolveStep, long[]>> perStudent = new HashMap<>();
        for (Object[] row : submissions.countByStudentAssignmentAndStep(assignmentId)) {
            perStudent.computeIfAbsent((Long) row[0], k -> new EnumMap<>(SolveStep.class))
                    .put((SolveStep) row[1],
                         new long[]{((Number) row[2]).longValue(), ((Number) row[3]).longValue()});
        }

        int problemCount = assignment.getProblemCount();
        List<StudentRow> rows = new ArrayList<>();
        for (StudentAssignment sa : studentAssignments.findWithStudentByAssignmentId(assignmentId)) {
            Map<SolveStep, long[]> steps = perStudent.getOrDefault(sa.getId(), Map.of());
            List<Rate> byStep = new ArrayList<>();
            long correct = 0;
            for (SolveStep step : SolveStep.values()) {
                long[] c = steps.getOrDefault(step, new long[]{0, 0});
                correct += c[0];
                // 분모는 '낸 것'이 아니라 '내야 할 것' — 안 푼 문제도 못 맞힌 것이다
                byStep.add(Rate.of(step.name(), step.label(), c[0], problemCount));
            }
            rows.add(new StudentRow(sa.getStudent().getId(), sa.getStudent().getName(), sa.status(),
                    percent(correct, assignment.totalSteps()), byStep));
        }
        rows.sort(Comparator.comparingInt(StudentRow::percent));
        return rows;
    }

    @SuppressWarnings("unchecked")
    private <K> Map<K, long[]> toMap(List<Object[]> rows) {
        Map<K, long[]> map = new LinkedHashMap<>();
        for (Object[] row : rows) {
            map.put((K) row[0], new long[]{((Number) row[1]).longValue(), ((Number) row[2]).longValue()});
        }
        return map;
    }

    private int percent(long correct, long total) {
        return total == 0 ? 0 : Math.round(correct * 100f / total);
    }

    private String adviceFor(ReportDtos.Rate weakest) {
        if (weakest == null) {
            return "아직 푼 아이가 없습니다.";
        }
        return switch (SolveStep.valueOf(weakest.key())) {
            case CUE -> "문장에서 단서를 찾는 연습이 더 필요합니다. 계산보다 읽기를 먼저 보세요.";
            case OPERATION -> "단서는 찾지만 어떤 연산인지 연결하지 못합니다. 동사와 기호를 짝지어 주세요.";
            case EXPRESSION -> "식으로 옮기는 단계에서 막힙니다. 숫자 순서를 헷갈리고 있습니다.";
            case ANSWER -> "식은 세우는데 계산에서 틀립니다. 연산 연습이 필요합니다.";
        };
    }
}
