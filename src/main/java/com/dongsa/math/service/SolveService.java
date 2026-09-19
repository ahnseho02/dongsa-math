package com.dongsa.math.service;

import com.dongsa.math.common.ApiException;
import com.dongsa.math.common.ErrorCode;
import com.dongsa.math.domain.Assignment;
import com.dongsa.math.domain.StudentAssignment;
import com.dongsa.math.domain.Submission;
import com.dongsa.math.problem.*;
import com.dongsa.math.repository.StudentAssignmentRepository;
import com.dongsa.math.repository.SubmissionRepository;
import com.dongsa.math.security.LoginUser;
import com.dongsa.math.web.dto.SolveDtos.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 아이가 문제를 푸는 쪽.
 *
 * 여기서 지키는 원칙 하나: **정답은 서버 밖으로 나가지 않는다.**
 * 문제를 내려보낼 때 단서도 정답도 연산도 빼고, 채점은 서버가 시드로 문제를 다시 만들어서 한다.
 * 개발자 도구를 열 초등학생은 없겠지만, 클라이언트를 믿지 않는 설계는 공짜로 얻어지지 않는다.
 */
@Service
@Transactional(readOnly = true)
public class SolveService {

    private final StudentAssignmentRepository studentAssignments;
    private final SubmissionRepository submissions;
    private final WorksheetFactory worksheets;
    private final StepGrader grader;

    public SolveService(StudentAssignmentRepository studentAssignments, SubmissionRepository submissions,
                        WorksheetFactory worksheets, StepGrader grader) {
        this.studentAssignments = studentAssignments;
        this.submissions = submissions;
        this.worksheets = worksheets;
        this.grader = grader;
    }

    public List<MyAssignment> myAssignments(LoginUser me) {
        return studentAssignments.findWithAssignmentByStudentId(me.id()).stream()
                .map(this::toMyAssignment)
                .toList();
    }

    public MyAssignmentDetail open(LoginUser me, Long studentAssignmentId) {
        StudentAssignment sa = mine(me, studentAssignmentId);
        List<GeneratedProblem> problems = problemsOf(sa);

        Map<Integer, Map<SolveStep, Submission>> done = submissionsByProblem(sa.getId());

        List<MyProblem> items = new ArrayList<>(problems.size());
        for (int i = 0; i < problems.size(); i++) {
            int no = i + 1;
            items.add(toMyProblem(no, problems.get(i), done.getOrDefault(no, Map.of())));
        }
        return new MyAssignmentDetail(toMyAssignment(sa), items);
    }

    /**
     * 한 단계 제출.
     *
     * 같은 단계가 또 들어오면 처음 결과를 그대로 돌려준다. 폰은 잘 끊기고,
     * 끊겼다고 다시 눌렀을 때 점수가 달라지면 안 되기 때문이다.
     */
    @Transactional
    public AnswerResult submit(LoginUser me, Long studentAssignmentId, AnswerRequest req) {
        StudentAssignment sa = mine(me, studentAssignmentId);
        Assignment assignment = sa.getAssignment();
        Instant now = Instant.now();

        if (!assignment.acceptsAnswers(now)) {
            throw new ApiException(ErrorCode.ASSIGNMENT_CLOSED);
        }
        int no = req.problemNo();
        if (no < 1 || no > assignment.getProblemCount()) {
            throw new ApiException(ErrorCode.PROBLEM_NOT_FOUND);
        }

        Submission already = submissions
                .findByStudentAssignmentIdAndProblemNoAndStep(sa.getId(), no, req.step())
                .orElse(null);
        if (already != null) {
            GeneratedProblem problem = problemsOf(sa).get(no - 1);
            return result(sa, assignment, no, req.step(), already.isCorrect(),
                    grader.correctValueOf(problem, req.step()));
        }

        requirePreviousStepsDone(sa.getId(), no, req.step());

        GeneratedProblem problem = problemsOf(sa).get(no - 1);
        boolean correct = grader.isCorrect(problem, req.step(), req.value(), req.numbers());

        submissions.save(new Submission(sa, no, req.step(),
                grader.normalize(req.step(), req.value(), req.numbers()),
                correct, problem.category(), now));

        sa.markStarted(now);
        if (submissions.countByStudentAssignmentId(sa.getId()) >= assignment.totalSteps()) {
            sa.markCompleted(now);
        }
        return result(sa, assignment, no, req.step(), correct, grader.correctValueOf(problem, req.step()));
    }

    // ── 내부 ──

    private StudentAssignment mine(LoginUser me, Long id) {
        return studentAssignments.findByIdAndStudentId(id, me.id())
                .orElseThrow(() -> new ApiException(ErrorCode.ASSIGNMENT_NOT_FOUND));
    }

    /**
     * 문제를 다시 만든다. 저장해 둔 것이 아니라 씨앗으로 매번 만들어 낸다.
     * 한 장이 최대 50문제라 비용은 무시할 수준이고, 대신 문제 데이터를 한 줄도 저장하지 않는다.
     */
    private List<GeneratedProblem> problemsOf(StudentAssignment sa) {
        return worksheets.build(sa.getAssignment().specWithSeed(sa.getSeed())).problems();
    }

    private void requirePreviousStepsDone(Long studentAssignmentId, int problemNo, SolveStep step) {
        List<Submission> ofProblem = submissions.findByStudentAssignmentIdAndProblemNo(studentAssignmentId, problemNo);
        for (SolveStep earlier : SolveStep.values()) {
            if (!step.comesAfter(earlier)) {
                continue;
            }
            boolean done = ofProblem.stream().anyMatch(s -> s.getStep() == earlier);
            if (!done) {
                throw new ApiException(ErrorCode.STEP_OUT_OF_ORDER);
            }
        }
    }

    private Map<Integer, Map<SolveStep, Submission>> submissionsByProblem(Long studentAssignmentId) {
        Map<Integer, Map<SolveStep, Submission>> map = new java.util.HashMap<>();
        for (Submission s : submissions.findByStudentAssignmentIdOrderByProblemNoAscStepAsc(studentAssignmentId)) {
            map.computeIfAbsent(s.getProblemNo(), k -> new EnumMap<>(SolveStep.class)).put(s.getStep(), s);
        }
        return map;
    }

    private MyProblem toMyProblem(int no, GeneratedProblem problem, Map<SolveStep, Submission> done) {
        List<StepState> steps = new ArrayList<>(SolveStep.COUNT);
        for (SolveStep step : SolveStep.values()) {
            Submission s = done.get(step);
            steps.add(new StepState(
                    step.name(), step.label(),
                    s != null,
                    s == null ? null : s.isCorrect(),
                    s == null ? null : s.getSubmittedValue(),
                    // 이미 낸 단계만 정답을 알려 준다
                    s == null ? null : grader.correctValueOf(problem, step)));
        }
        return new MyProblem(no, problem.category().label(), problem.sentence(),
                problem.tappableWords(), problem.numbers(), problem.unit(), steps);
    }

    private MyAssignment toMyAssignment(StudentAssignment sa) {
        Assignment a = sa.getAssignment();
        return new MyAssignment(
                sa.getId(), a.getTitle(), a.getProblemCount(), a.totalSteps(),
                submissions.countByStudentAssignmentId(sa.getId()),
                submissions.countByStudentAssignmentIdAndCorrectTrue(sa.getId()),
                sa.status(), a.getDueAt(), a.acceptsAnswers(Instant.now()));
    }

    private AnswerResult result(StudentAssignment sa, Assignment assignment,
                                int no, SolveStep step, boolean correct, String correctValue) {
        long answered = submissions.countByStudentAssignmentId(sa.getId());
        long correctCount = submissions.countByStudentAssignmentIdAndCorrectTrue(sa.getId());
        SolveStep next = step == SolveStep.ANSWER ? null : SolveStep.values()[step.ordinal() + 1];
        boolean problemDone = next == null;
        return new AnswerResult(no, step.name(), correct, correctValue,
                next == null ? null : next.name(),
                problemDone, answered >= assignment.totalSteps(),
                answered, correctCount, assignment.totalSteps());
    }
}
