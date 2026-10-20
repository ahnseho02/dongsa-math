package com.dongsa.math;

import com.dongsa.math.problem.*;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * 도전 문제 — 두 단계, 거꾸로, 함정 숫자.
 * 셋 다 "계산" 이 아니라 "읽기" 를 시험한다. 이 앱이 하려는 것이 그것이다.
 */
@DisplayName("도전 문제")
class ChallengeProblemTest extends ApiTestSupport {

    @Autowired WorksheetFactory factory;
    @Autowired StepGrader grader;

    private static final List<ProblemCategory> ALL = List.of(ProblemCategory.values());

    private List<GeneratedProblem> build(int grade, boolean challenge, List<ProblemCategory> cats, long seed) {
        return factory.build(new WorksheetSpec(grade, cats, 20, true, challenge, seed)).problems();
    }

    @Test
    @DisplayName("도전을 끄면 도전 문제가 하나도 안 나온다 — 기존 학습지는 그대로다")
    void basicSheetsHaveNoHardProblems() {
        for (int grade = 1; grade <= 6; grade++) {
            for (long seed = 0; seed < 20; seed++) {
                for (GeneratedProblem p : build(grade, false, factory.categoriesFor(grade, false), seed)) {
                    assertThat(p.hard()).as("초%d 기본 학습지에 %s", grade, p.templateCode()).isFalse();
                    assertThat(p.isTwoStep()).isFalse();
                    assertThat(p.distractors()).isEmpty();
                }
            }
        }
        assertThat(factory.categoriesFor(3, false))
                .doesNotContain(ProblemCategory.TWO_STEP, ProblemCategory.INVERSE);
    }

    @Test
    @DisplayName("도전을 켜면 두 단계·거꾸로 유형이 열리고 함정 문제가 섞인다")
    void challengeOpensNewKinds() {
        assertThat(factory.categoriesFor(3, true))
                .contains(ProblemCategory.TWO_STEP, ProblemCategory.INVERSE);

        boolean sawTwoStep = false, sawInverse = false, sawDistractor = false;
        for (long seed = 0; seed < 30; seed++) {
            for (GeneratedProblem p : build(3, true, factory.categoriesFor(3, true), seed)) {
                sawTwoStep |= p.isTwoStep();
                sawInverse |= p.category() == ProblemCategory.INVERSE;
                sawDistractor |= !p.distractors().isEmpty();
            }
        }
        assertThat(sawTwoStep).isTrue();
        assertThat(sawInverse).isTrue();
        assertThat(sawDistractor).isTrue();
    }

    @Test
    @DisplayName("두 단계 문제는 답이 (첫째 연산) 그다음 (둘째 연산) 으로 계산된다")
    void twoStepComputesInOrder() {
        for (long seed = 0; seed < 40; seed++) {
            for (GeneratedProblem p : build(3, true, List.of(ProblemCategory.TWO_STEP), seed)) {
                assertThat(p.isTwoStep()).isTrue();
                assertThat(p.numbers()).hasSize(3);
                Num first = p.operations().get(0).apply(p.numbers().subList(0, 2));
                Num expected = p.operations().get(1).apply(List.of(first, p.numbers().get(2)));
                assertThat(p.answer()).as(p.expression()).isEqualTo(expected);
                assertThat(p.answer().isNegative()).as("%s 답이 음수", p.expression()).isFalse();
                assertThat(p.expression()).startsWith("(").contains(") ");
                assertThat(p.operationsText()).contains(",");
            }
        }
    }

    @Test
    @DisplayName("고등어 420마리, 갈치는 135마리 적다 → (420 − 135) + 420")
    void lessThenSumUsesTheSameNumberTwice() {
        boolean checked = false;
        for (long seed = 0; seed < 60 && !checked; seed++) {
            for (GeneratedProblem p : build(3, true, List.of(ProblemCategory.TWO_STEP), seed)) {
                if (!p.templateCode().equals("t2_less")) continue;
                assertThat(p.numbers().get(0)).isEqualTo(p.numbers().get(2));
                assertThat(p.operations()).containsExactly(Operation.SUBTRACT, Operation.ADD);
                // 문장에 420 과 135 만 나오지만 식에는 420 이 두 번 들어간다
                assertThat(p.shownNumberTexts()).hasSize(3);
                checked = true;
                break;
            }
        }
        assertThat(checked).isTrue();
    }

    @Test
    @DisplayName("두 단계는 연산을 순서대로 둘 다 맞혀야 한다")
    void twoStepOperationGrading() {
        GeneratedProblem p = firstOf(3, List.of(ProblemCategory.TWO_STEP), "t2_less");
        assertThat(grader.isCorrect(p, SolveStep.OPERATION, "−,+", null)).isTrue();
        assertThat(grader.isCorrect(p, SolveStep.OPERATION, "-,+", null)).as("하이픈도 받는다").isTrue();
        assertThat(grader.isCorrect(p, SolveStep.OPERATION, "+,−", null)).as("순서가 바뀌면 틀림").isFalse();
        assertThat(grader.isCorrect(p, SolveStep.OPERATION, "−", null)).as("하나만 고르면 틀림").isFalse();
        assertThat(grader.correctValueOf(p, SolveStep.OPERATION)).contains("먼저").contains("그다음");
    }

    @Test
    @DisplayName("두 단계 식은 괄호 안팎이 정해져 있어 순서를 바꾸면 틀린다")
    void twoStepExpressionOrderMatters() {
        GeneratedProblem p = firstOf(3, List.of(ProblemCategory.TWO_STEP), "t2_more");   // (a + b) + a
        List<String> right = p.numberTexts();
        List<String> swapped = List.of(right.get(1), right.get(0), right.get(2));
        assertThat(grader.isCorrect(p, SolveStep.EXPRESSION, null, right)).isTrue();
        // 덧셈만 있어도 괄호 구조가 있으니 순서를 지켜야 한다
        assertThat(grader.isCorrect(p, SolveStep.EXPRESSION, null, swapped)).isFalse();
    }

    @Test
    @DisplayName("거꾸로 문제는 단서는 '더했더니' 인데 식은 빼기다")
    void inverseFlipsTheOperation() {
        for (long seed = 0; seed < 40; seed++) {
            for (GeneratedProblem p : build(3, true, List.of(ProblemCategory.INVERSE), seed)) {
                if (p.templateCode().startsWith("inv_add")) {
                    assertThat(p.operation()).as(p.plainSentence()).isEqualTo(Operation.SUBTRACT);
                    assertThat(p.cues()).anyMatch(c -> c.contains("더"));
                } else {
                    assertThat(p.operation()).as(p.plainSentence()).isEqualTo(Operation.ADD);
                    assertThat(p.cues()).anyMatch(c -> c.contains("뺐") || c.contains("쓰고"));
                }
                // 거꾸로 계산해서 나온 '어떤 수' 를 문장대로 다시 계산하면 문장의 결과가 나와야 한다
                Num some = p.answer();
                Num b = p.numbers().get(1);
                Num shown = p.numbers().get(0);
                Num back = p.operation() == Operation.SUBTRACT ? some.plus(b) : some.minus(b);
                assertThat(back).as("%s 를 거꾸로 확인", p.plainSentence()).isEqualTo(shown);
                assertThat(p.answer().isNegative()).isFalse();
            }
        }
    }

    @Test
    @DisplayName("함정 숫자는 문장에는 있고 식에는 없다 — 넣으면 틀린다")
    void distractorsMustBeLeftOut() {
        GeneratedProblem p = null;
        for (long seed = 0; seed < 60 && p == null; seed++) {
            for (GeneratedProblem q : build(3, true, List.of(ProblemCategory.DECREASE), seed)) {
                if (!q.distractors().isEmpty()) { p = q; break; }
            }
        }
        assertThat(p).isNotNull();
        String trap = p.distractors().get(0).text();
        assertThat(p.plainSentence()).as("함정 숫자가 문장에 있다").contains(trap);
        assertThat(p.expression()).as("식에는 없다").doesNotContain(trap);
        assertThat(p.shownNumberTexts()).as("아이가 받는 칩에는 함정이 섞여 있다").contains(trap);

        assertThat(grader.isCorrect(p, SolveStep.EXPRESSION, null, p.numberTexts())).isTrue();
        List<String> withTrap = new ArrayList<>(p.numberTexts());
        withTrap.add(trap);
        assertThat(grader.isCorrect(p, SolveStep.EXPRESSION, null, withTrap)).as("함정을 넣으면 틀림").isFalse();
        List<String> trapInstead = List.of(p.numberTexts().get(0), trap);
        assertThat(grader.isCorrect(p, SolveStep.EXPRESSION, null, trapInstead)).as("함정으로 바꿔 넣어도 틀림").isFalse();
    }

    @Test
    @DisplayName("과제에 도전을 켜면 아이 화면에 두 단계 문제가 그대로 내려온다")
    void challengeAssignmentEndToEnd() throws Exception {
        Signup owner = signupOwner("ch1@example.com", "학원");
        long studentId = bodyOf(call(post("/api/students"), owner.token(),
                Map.of("name", "김민우", "grade", 3, "pin", "1234"))).get("student").get("id").asLong();

        JsonNode created = bodyOf(call(post("/api/assignments"), owner.token(), new HashMap<>(Map.of(
                "title", "도전", "grade", 3, "categories", List.of("TWO_STEP"), "count", 3,
                "challenge", true, "sameForEveryone", true, "studentIds", List.of(studentId)))));
        assertThat(created.get("assignment").get("challenge").asBoolean()).isTrue();
        String code = created.get("assignment").get("code").asText();

        JsonNode key = bodyOf(call(post("/api/worksheets"), owner.token(), new HashMap<>(Map.of(
                "grade", 3, "categories", List.of("TWO_STEP"), "count", 3, "carry", true,
                "challenge", true, "code", code)))).get("problems");

        String token = bodyOf(call(post("/api/auth/student/login"), null,
                Map.of("academyCode", owner.academyCode(), "name", "김민우", "pin", "1234"))).get("token").asText();
        long said = bodyOf(call(get("/api/my/assignments"), token, null)).get(0).get("id").asLong();
        JsonNode mine = bodyOf(call(get("/api/my/assignments/" + said), token, null));

        JsonNode first = mine.get("problems").get(0);
        assertThat(first.get("operationCount").asInt()).isEqualTo(2);
        assertThat(first.get("operandCount").asInt()).isEqualTo(3);
        assertThat(first.has("answer")).isFalse();

        // 끝까지 푼다
        JsonNode p = key.get(0);
        List<String> nums = new ArrayList<>();
        p.get("numbers").forEach(n -> nums.add(n.asText()));
        Map<String, Object> cue = new HashMap<>(Map.of("problemNo", 1, "step", "CUE", "value", p.get("cues").get(0).asText()));
        assertThat(bodyOf(call(post("/api/my/assignments/" + said + "/answers"), token, cue)).get("correct").asBoolean()).isTrue();
        Map<String, Object> op = new HashMap<>(Map.of("problemNo", 1, "step", "OPERATION", "value", p.get("operation").asText()));
        assertThat(bodyOf(call(post("/api/my/assignments/" + said + "/answers"), token, op)).get("correct").asBoolean()).isTrue();
        Map<String, Object> ex = new HashMap<>(Map.of("problemNo", 1, "step", "EXPRESSION", "numbers", nums));
        JsonNode exRes = bodyOf(call(post("/api/my/assignments/" + said + "/answers"), token, ex));
        assertThat(exRes.get("correct").asBoolean()).isTrue();
        assertThat(exRes.get("correctValue").asText()).startsWith("(");
        Map<String, Object> an = new HashMap<>(Map.of("problemNo", 1, "step", "ANSWER", "value", p.get("answer").asText()));
        assertThat(bodyOf(call(post("/api/my/assignments/" + said + "/answers"), token, an)).get("correct").asBoolean()).isTrue();
    }

    private GeneratedProblem firstOf(int grade, List<ProblemCategory> cats, String code) {
        for (long seed = 0; seed < 100; seed++) {
            for (GeneratedProblem p : build(grade, true, cats, seed)) {
                if (p.templateCode().equals(code)) return p;
            }
        }
        throw new AssertionError(code + " 를 못 찾음");
    }
}
