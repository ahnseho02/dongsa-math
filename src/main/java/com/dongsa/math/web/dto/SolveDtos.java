package com.dongsa.math.web.dto;

import com.dongsa.math.problem.SolveStep;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;

public final class SolveDtos {

    private SolveDtos() {}

    public record MyAssignment(
            Long id,
            String title,
            int problemCount,
            int totalSteps,
            long answeredSteps,
            long correctSteps,
            String status,
            Instant dueAt,
            boolean open) {}

    /**
     * 아이에게 내려보내는 문제.
     * 단서(cues)도 정답(answer)도 연산(operation)도 들어 있지 않다 — 그게 바로 물어볼 것들이다.
     */
    public record MyProblem(
            int no,
            String categoryLabel,
            String sentence,
            List<String> tappableWords,
            List<Integer> numbers,
            String unit,
            List<StepState> steps) {}

    /** 이미 낸 단계만 정답이 함께 온다. 아직 안 낸 단계는 correctValue 가 비어 있다. */
    public record StepState(
            String step,
            String label,
            boolean submitted,
            Boolean correct,
            String submittedValue,
            String correctValue) {}

    public record MyAssignmentDetail(MyAssignment assignment, List<MyProblem> problems) {}

    public record AnswerRequest(
            @NotNull @Min(1) Integer problemNo,
            @NotNull SolveStep step,
            String value,
            List<Integer> numbers) {}

    public record AnswerResult(
            int problemNo,
            String step,
            boolean correct,
            String correctValue,
            String nextStep,
            boolean problemDone,
            boolean assignmentDone,
            long answeredSteps,
            long correctSteps,
            int totalSteps) {}
}
