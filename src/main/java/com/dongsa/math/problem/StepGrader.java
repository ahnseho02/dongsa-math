package com.dongsa.math.problem;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 단계별 채점.
 *
 * 채점은 반드시 서버에서 한다. 학생에게 내려보내는 문제에는 정답도 단서도 들어 있지 않고,
 * 서버가 시드로 문제를 다시 만들어서 맞춰 본다.
 */
@Component
public class StepGrader {

    /** 클라이언트가 어떤 기호를 보내든 알아듣는다. 곱셈 기호 하나 때문에 오답 처리되면 안 된다. */
    public static Operation parseOperation(String raw) {
        if (raw == null) {
            return null;
        }
        return switch (raw.strip()) {
            case "+", "＋" -> Operation.ADD;
            case "-", "−", "–", "—", "－" -> Operation.SUBTRACT;
            case "*", "x", "X", "×", "✕" -> Operation.MULTIPLY;
            case "/", "÷", "∕" -> Operation.DIVIDE;
            default -> null;
        };
    }

    public boolean isCorrect(GeneratedProblem problem, SolveStep step, String value, List<Integer> numbers) {
        return switch (step) {
            case CUE -> value != null && problem.isCue(value.strip());
            case OPERATION -> problem.operation() == parseOperation(value);
            case EXPRESSION -> expressionMatches(problem, numbers);
            case ANSWER -> parseInt(value) != null && parseInt(value) == problem.answer();
        };
    }

    /**
     * 뺄셈과 나눗셈은 순서가 답을 바꾸므로 그대로 맞아야 하고,
     * 덧셈과 곱셈은 순서를 따지지 않는다 — 458 + 342 도 맞는 식이다.
     */
    private boolean expressionMatches(GeneratedProblem problem, List<Integer> submitted) {
        if (submitted == null || submitted.size() != problem.numbers().size()) {
            return false;
        }
        if (problem.operation().orderMatters()) {
            return submitted.equals(problem.numbers());
        }
        List<Integer> a = new ArrayList<>(submitted);
        List<Integer> b = new ArrayList<>(problem.numbers());
        a.sort(null);
        b.sort(null);
        return a.equals(b);
    }

    /** 틀렸을 때 아이에게 보여 줄 정답. 그 단계를 이미 제출한 뒤에만 내려보낸다. */
    public String correctValueOf(GeneratedProblem problem, SolveStep step) {
        return switch (step) {
            case CUE -> String.join(", ", problem.cues());
            case OPERATION -> problem.operation().sign();
            case EXPRESSION -> problem.expression();
            case ANSWER -> problem.answer() + problem.unit();
        };
    }

    /** DB 에 남길 값. 나중에 "무엇을 골랐길래 틀렸는지" 보려면 원래 입력이 필요하다. */
    public String normalize(SolveStep step, String value, List<Integer> numbers) {
        if (step == SolveStep.EXPRESSION) {
            return numbers == null ? "" : numbers.stream().map(String::valueOf)
                    .reduce((x, y) -> x + "," + y).orElse("");
        }
        return value == null ? "" : value.strip();
    }

    private Integer parseInt(String value) {
        try {
            return Integer.valueOf(value.strip());
        } catch (NumberFormatException | NullPointerException e) {
            return null;
        }
    }
}
