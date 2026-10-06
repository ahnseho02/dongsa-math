package com.dongsa.math;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * 초4~6 은 분수와 소수를 다룬다.
 * 아이가 "3/4" 나 "1.5" 를 적었을 때 제대로 채점되는지, 끝까지 풀어서 확인한다.
 */
@DisplayName("분수·소수 문제 풀기")
class FractionAndDecimalSolveTest extends ApiTestSupport {

    private record Scene(Signup owner, String studentToken, long studentAssignmentId, JsonNode key) {}

    private Scene assign(String email, int grade, List<String> categories, int count) throws Exception {
        Signup owner = signupOwner(email, "학원");
        long studentId = bodyOf(call(post("/api/students"), owner.token(),
                Map.of("name", "김민우", "grade", grade, "pin", "1234"))).get("student").get("id").asLong();

        JsonNode created = bodyOf(call(post("/api/assignments"), owner.token(), new HashMap<>(Map.of(
                "title", "초" + grade + " 숙제", "grade", grade, "categories", categories,
                "count", count, "sameForEveryone", true, "studentIds", List.of(studentId)))));
        String code = created.get("assignment").get("code").asText();

        JsonNode key = bodyOf(call(post("/api/worksheets"), owner.token(), new HashMap<>(Map.of(
                "grade", grade, "categories", categories, "count", count, "carry", true, "code", code))))
                .get("problems");

        String token = bodyOf(call(post("/api/auth/student/login"), null,
                Map.of("academyCode", owner.academyCode(), "name", "김민우", "pin", "1234")))
                .get("token").asText();
        long said = bodyOf(call(get("/api/my/assignments"), token, null)).get(0).get("id").asLong();
        return new Scene(owner, token, said, key);
    }

    private JsonNode answer(Scene s, int no, String step, String value, List<String> numbers) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("problemNo", no);
        body.put("step", step);
        if (value != null) body.put("value", value);
        if (numbers != null) body.put("numbers", numbers);
        return bodyOf(call(post("/api/my/assignments/" + s.studentAssignmentId() + "/answers"),
                s.studentToken(), body));
    }

    private List<String> numbersOf(JsonNode problem) {
        List<String> out = new ArrayList<>();
        problem.get("numbers").forEach(n -> out.add(n.asText()));
        return out;
    }

    @Test
    @DisplayName("초6 문제를 끝까지 풀면 분수·소수 답이 그대로 채점된다")
    void solveSixthGradeToTheEnd() throws Exception {
        Scene s = assign("fd1@example.com", 6,
                List.of("MERGE", "DECREASE", "GROUP", "SHARE", "RATIO"), 10);

        boolean sawFraction = false;
        boolean sawDecimal = false;
        for (int i = 0; i < s.key().size(); i++) {
            JsonNode p = s.key().get(i);
            int no = i + 1;
            String expected = p.get("answer").asText();
            sawFraction |= expected.contains("/") || numbersOf(p).stream().anyMatch(n -> n.contains("/"));
            sawDecimal |= expected.contains(".") || numbersOf(p).stream().anyMatch(n -> n.contains("."));

            assertThat(answer(s, no, "CUE", p.get("cues").get(0).asText(), null).get("correct").asBoolean())
                    .as("%d번 단서", no).isTrue();
            assertThat(answer(s, no, "OPERATION", p.get("operation").asText(), null).get("correct").asBoolean())
                    .as("%d번 연산", no).isTrue();
            assertThat(answer(s, no, "EXPRESSION", null, numbersOf(p)).get("correct").asBoolean())
                    .as("%d번 식 %s", no, p.get("expression").asText()).isTrue();
            assertThat(answer(s, no, "ANSWER", expected, null).get("correct").asBoolean())
                    .as("%d번 답 %s = %s", no, p.get("expression").asText(), expected).isTrue();
        }
        assertThat(sawFraction).as("초6 문제에 분수가 나와야 한다").isTrue();
        assertThat(sawDecimal).as("초6 문제에 소수가 나와야 한다").isTrue();
    }

    @Test
    @DisplayName("0.5 로 써도 1/2 로 써도 맞다 — 값이 같으면 맞다")
    void equivalentFormsAreAccepted() throws Exception {
        Scene s = assign("fd2@example.com", 5, List.of("MERGE", "DECREASE"), 12);

        int checked = 0;
        for (int i = 0; i < s.key().size(); i++) {
            JsonNode p = s.key().get(i);
            String expected = p.get("answer").asText();
            if (!expected.contains("/")) {
                continue;
            }
            // 1/2 로 나오는 답을 0.5 라고 적어도 맞아야 한다.
            // 1/3 처럼 소수로 딱 떨어지지 않는 것은 애초에 소수로 적을 수가 없으니 건너뛴다
            // (분모의 소인수가 2 와 5 뿐일 때만 유한소수가 된다).
            String[] parts = expected.split("/");
            long den = Long.parseLong(parts[1]);
            long reduced = den;
            while (reduced % 2 == 0) { reduced /= 2; }
            while (reduced % 5 == 0) { reduced /= 5; }
            if (reduced != 1) {
                continue;
            }
            String decimalText = new java.math.BigDecimal(parts[0])
                    .divide(new java.math.BigDecimal(parts[1]), 10, java.math.RoundingMode.HALF_UP)
                    .stripTrailingZeros().toPlainString();

            int no = i + 1;
            answer(s, no, "CUE", p.get("cues").get(0).asText(), null);
            answer(s, no, "OPERATION", p.get("operation").asText(), null);
            answer(s, no, "EXPRESSION", null, numbersOf(p));
            assertThat(answer(s, no, "ANSWER", decimalText, null).get("correct").asBoolean())
                    .as("%s 를 %s 로 적어도 맞아야 한다", expected, decimalText).isTrue();
            checked++;
        }
        assertThat(checked).as("분수 답이 나오는 문제가 하나는 있어야 한다").isPositive();
    }

    @Test
    @DisplayName("기약분수가 아니게 적어도 값이 같으면 맞다 — 2/4 와 1/2")
    void unreducedFractionsAreAccepted() throws Exception {
        Scene s = assign("fd3@example.com", 5, List.of("MERGE", "DECREASE"), 12);

        for (int i = 0; i < s.key().size(); i++) {
            JsonNode p = s.key().get(i);
            String expected = p.get("answer").asText();
            if (!expected.contains("/")) {
                continue;
            }
            String[] parts = expected.split("/");
            String doubled = (Long.parseLong(parts[0]) * 2) + "/" + (Long.parseLong(parts[1]) * 2);

            int no = i + 1;
            answer(s, no, "CUE", p.get("cues").get(0).asText(), null);
            answer(s, no, "OPERATION", p.get("operation").asText(), null);
            answer(s, no, "EXPRESSION", null, numbersOf(p));
            assertThat(answer(s, no, "ANSWER", doubled, null).get("correct").asBoolean())
                    .as("%s 를 %s 로 적어도 맞아야 한다", expected, doubled).isTrue();
            return;
        }
    }

    @Test
    @DisplayName("비율 문제는 문장에 '30 퍼센트', 식에는 0.3 으로 나온다")
    void percentReadsAsPercentButComputesAsDecimal() throws Exception {
        Signup owner = signupOwner("fd4@example.com", "학원");
        JsonNode problems = bodyOf(call(post("/api/worksheets"), owner.token(), new HashMap<>(Map.of(
                "grade", 6, "categories", List.of("RATIO"), "count", 10, "carry", true))))
                .get("problems");

        boolean sawPercent = false;
        for (JsonNode p : problems) {
            String sentence = p.get("plainSentence").asText();
            if (!sentence.contains("퍼센트")) {
                continue;
            }
            sawPercent = true;
            assertThat(sentence).as("문장에 0.3 같은 소수가 그대로 보이면 안 된다")
                    .doesNotContain("0.");
            assertThat(p.get("expression").asText()).as("식에는 소수로 들어간다").contains("0.");
            assertThat(p.get("answer").asText()).as("사람 수는 정수로 떨어져야 한다").doesNotContain(".");
        }
        assertThat(sawPercent).isTrue();
    }

    @Test
    @DisplayName("엉뚱하게 적으면 틀린다 — 읽을 수 없는 답")
    void garbageIsWrong() throws Exception {
        Scene s = assign("fd5@example.com", 6, List.of("DECREASE"), 3);
        JsonNode p = s.key().get(0);

        answer(s, 1, "CUE", p.get("cues").get(0).asText(), null);
        answer(s, 1, "OPERATION", p.get("operation").asText(), null);
        answer(s, 1, "EXPRESSION", null, numbersOf(p));
        JsonNode r = answer(s, 1, "ANSWER", "1/0", null);
        assertThat(r.get("correct").asBoolean()).isFalse();
        assertThat(r.get("correctValue").asText()).isEqualTo(p.get("answer").asText() + p.get("unit").asText());
    }
}
