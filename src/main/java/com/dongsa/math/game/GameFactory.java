package com.dongsa.math.game;

import com.dongsa.math.problem.GradeRange;
import com.dongsa.math.problem.Operation;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 게임 문제 만들기.
 *
 * 학습지와 같은 규칙을 따른다 — 씨앗이 같으면 같은 문제가 나온다.
 * 그래서 문제를 저장하지 않아도 되고, 무엇보다 **채점을 서버가 다시 만들어서 한다.**
 * 답을 클라이언트에 내려보내지 않으니 점수를 꾸밀 수 없다.
 */
@Component
public class GameFactory {

    public static final int PROBLEM_COUNT = 40;   // 60초 안에 다 풀기는 어렵게
    public static final int LIMIT_SECONDS = 60;

    public List<GameProblem> build(int grade, long seed) {
        Random rnd = new Random(seed);
        GradeRange range = GradeRange.of(grade);
        List<GameProblem> problems = new ArrayList<>(PROBLEM_COUNT);

        for (int i = 0; i < PROBLEM_COUNT; i++) {
            problems.add(one(i + 1, grade, range, rnd));
        }
        return problems;
    }

    private GameProblem one(int no, int grade, GradeRange range, Random rnd) {
        Operation operation = pickOperation(grade, rnd);
        return switch (operation) {
            case ADD -> {
                int a = between(range, rnd), b = between(range, rnd);
                yield new GameProblem(no, a, b, operation, a + b);
            }
            case SUBTRACT -> {
                int a = between(range, rnd);
                int b = 1 + rnd.nextInt(Math.max(1, a - 1));   // 답이 음수가 되지 않게
                yield new GameProblem(no, a, b, operation, a - b);
            }
            case MULTIPLY -> {
                int a = 2 + rnd.nextInt(grade <= 2 ? 8 : 18);
                int b = 2 + rnd.nextInt(8);
                yield new GameProblem(no, a, b, operation, a * b);
            }
            case DIVIDE -> {
                int divisor = 2 + rnd.nextInt(8);
                int quotient = 2 + rnd.nextInt(grade <= 3 ? 10 : 20);
                yield new GameProblem(no, divisor * quotient, divisor, operation, quotient);
            }
        };
    }

    /** 배운 연산만 나온다. 곱셈은 2학년, 나눗셈은 3학년부터. */
    private Operation pickOperation(int grade, Random rnd) {
        List<Operation> allowed = new ArrayList<>(List.of(Operation.ADD, Operation.SUBTRACT));
        if (grade >= 2) {
            allowed.add(Operation.MULTIPLY);
        }
        if (grade >= 3) {
            allowed.add(Operation.DIVIDE);
        }
        return allowed.get(rnd.nextInt(allowed.size()));
    }

    /** 게임은 속도를 보는 것이라 학습지보다 수를 작게 잡는다. */
    private int between(GradeRange range, Random rnd) {
        int max = Math.min(range.max(), 999);
        int min = Math.min(range.min(), max);
        return min + rnd.nextInt(max - min + 1);
    }
}
