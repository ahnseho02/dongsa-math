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
            // 분수(3/4)와 소수(1.5)가 섞이므로 글자로 주고받는다.
            // 식에 쓰는 수와 함정 숫자가 함께 들어 있다 — 고르는 것이 문제의 일부다.
            List<String> numbers,
            /** 식에 들어가는 수의 개수. 함정이 있으면 numbers 보다 작다. */
            int operandCount,
            /** 1 또는 2. 두 단계 문제면 연산을 두 번 고른다. */
            int operationCount,
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
            List<String> numbers) {}

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
