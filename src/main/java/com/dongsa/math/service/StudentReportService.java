package com.dongsa.math.service;

import com.dongsa.math.common.ApiException;
import com.dongsa.math.common.ErrorCode;
import com.dongsa.math.domain.Assignment;
import com.dongsa.math.domain.Student;
import com.dongsa.math.domain.StudentAssignment;
import com.dongsa.math.problem.ProblemCategory;
import com.dongsa.math.problem.SolveStep;
import com.dongsa.math.repository.StudentAssignmentRepository;
import com.dongsa.math.repository.StudentRepository;
import com.dongsa.math.repository.SubmissionRepository;
import com.dongsa.math.security.LoginUser;
import com.dongsa.math.web.dto.ReportDtos.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 학생 성적.
 *
 * 과제 리포트가 "이번 숙제에서 누가 막혔나" 라면, 여기는 "이 아이가 그동안 뭘 못하나" 다.
 * 학원에서 학부모 상담할 때 쓰는 숫자가 이쪽이다.
 */
@Service
@Transactional(readOnly = true)
public class StudentReportService {

    private final StudentRepository students;
    private final StudentAssignmentRepository studentAssignments;
    private final SubmissionRepository submissions;

    public StudentReportService(StudentRepository students, StudentAssignmentRepository studentAssignments,
                                SubmissionRepository submissions) {
        this.students = students;
        this.studentAssignments = studentAssignments;
        this.submissions = submissions;
    }

    /** 학원 학생 전체 — 쿼리 두 번(학생 목록 + 집계 하나)으로 끝난다. */
    public List<AcademyStudentRow> academyOverview(LoginUser me) {
        Map<Long, Map<SolveStep, long[]>> perStudent = new HashMap<>();
        for (Object[] row : submissions.countByStudentAndStepInAcademy(me.academyId())) {
            perStudent.computeIfAbsent((Long) row[0], k -> new EnumMap<>(SolveStep.class))
                    .put((SolveStep) row[1], counts(row[2], row[3]));
        }

        List<AcademyStudentRow> rows = new ArrayList<>();
        for (Student student : students.findByAcademyIdOrderByNameAsc(me.academyId())) {
            Map<SolveStep, long[]> steps = perStudent.getOrDefault(student.getId(), Map.of());
            List<Rate> bySteps = new ArrayList<>();
            long correct = 0;
            long answered = 0;
            for (SolveStep step : SolveStep.values()) {
                long[] c = steps.getOrDefault(step, new long[]{0, 0});
                correct += c[0];
                answered += c[1];
                bySteps.add(Rate.of(step.name(), step.label(), c[0], c[1]));
            }
            rows.add(new AcademyStudentRow(student.getId(), student.getName(), student.getGrade(),
                    student.isActive(), studentAssignments.countByStudentId(student.getId()),
                    answered, percent(correct, answered), bySteps));
        }
        // 약한 아이가 위로 오게 — 아직 한 번도 안 푼 아이는 맨 아래
        rows.sort(Comparator.comparing((AcademyStudentRow r) -> r.answeredSteps() == 0)
                .thenComparingInt(AcademyStudentRow::percent));
        return rows;
    }

    public StudentReport forStudent(LoginUser me, Long studentId) {
        Student student = students.findByIdAndAcademyId(studentId, me.academyId())
                .orElseThrow(() -> new ApiException(ErrorCode.STUDENT_NOT_FOUND));

        List<Rate> bySteps = new ArrayList<>();
        Map<SolveStep, long[]> stepCounts = toMap(submissions.countByStepForStudent(studentId));
        long correct = 0;
        long answered = 0;
        for (SolveStep step : SolveStep.values()) {
            long[] c = stepCounts.getOrDefault(step, new long[]{0, 0});
            correct += c[0];
            answered += c[1];
            bySteps.add(Rate.of(step.name(), step.label(), c[0], c[1]));
        }

        List<Rate> byCategories = new ArrayList<>();
        toMap(submissions.countByCategoryForStudent(studentId)).forEach((category, c) ->
                byCategories.add(Rate.of(((ProblemCategory) category).name(),
                        ((ProblemCategory) category).label(), c[0], c[1])));
        byCategories.sort(Comparator.comparingInt(Rate::percent));

        Map<Long, long[]> perAssignment = new HashMap<>();
        for (Object[] row : submissions.countByStudentAssignmentForStudent(studentId)) {
            perAssignment.put((Long) row[0], counts(row[1], row[2]));
        }

        List<AssignmentScore> scores = new ArrayList<>();
        for (StudentAssignment sa : studentAssignments.findWithAssignmentByStudentId(studentId)) {
            Assignment assignment = sa.getAssignment();
            long[] c = perAssignment.getOrDefault(sa.getId(), new long[]{0, 0});
            int total = assignment.totalSteps();
            scores.add(new AssignmentScore(assignment.getId(), assignment.getTitle(), sa.status(),
                    c[0], total, percent(c[0], total), assignment.getCreatedAt()));
        }

        Rate weakest = bySteps.stream()
                .filter(r -> r.total() > 0)
                .min(Comparator.comparingInt(Rate::percent))
                .orElse(null);

        return new StudentReport(
                student.getId(), student.getName(), student.getGrade(), student.isActive(),
                studentAssignments.countByStudentId(studentId),
                studentAssignments.countByStudentIdAndCompletedAtIsNotNull(studentId),
                correct, answered, percent(correct, answered),
                bySteps, byCategories, scores,
                weakest == null ? null : weakest.key(),
                adviceFor(weakest));
    }

    // ── 내부 ──

    private long[] counts(Object correct, Object total) {
        return new long[]{((Number) correct).longValue(), ((Number) total).longValue()};
    }

    @SuppressWarnings("unchecked")
    private <K> Map<K, long[]> toMap(List<Object[]> rows) {
        Map<K, long[]> map = new LinkedHashMap<>();
        for (Object[] row : rows) {
            map.put((K) row[0], counts(row[1], row[2]));
        }
        return map;
    }

    private int percent(long correct, long total) {
        return total == 0 ? 0 : Math.round(correct * 100f / total);
    }

    private String adviceFor(Rate weakest) {
        if (weakest == null) {
            return "아직 푼 과제가 없습니다.";
        }
        return switch (SolveStep.valueOf(weakest.key())) {
            case CUE -> "문장에서 단서를 찾는 연습이 더 필요합니다. 계산보다 읽기를 먼저 보세요.";
            case OPERATION -> "단서는 찾지만 어떤 연산인지 연결하지 못합니다. 동사와 기호를 짝지어 주세요.";
            case EXPRESSION -> "식으로 옮기는 단계에서 막힙니다. 숫자 순서를 헷갈리고 있습니다.";
            case ANSWER -> "식은 세우는데 계산에서 틀립니다. 연산 연습이 필요합니다.";
        };
    }
}
