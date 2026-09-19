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
 * 리포트가 두 아이를 갈라내는지 확인한다.
 * 같은 점수라도 '문장을 못 읽는 아이'와 '계산을 못 하는 아이'는 처방이 다르다 —
 * 그걸 못 보여 주면 이 서비스는 그냥 채점기다.
 */
@DisplayName("과제 리포트")
class AssignmentReportTest extends ApiTestSupport {

    private static final List<String> CATEGORIES = List.of("MERGE", "DECREASE");
    private static final int COUNT = 4;

    @Test
    @DisplayName("문장을 못 읽는 아이와 계산을 못 하는 아이가 갈린다")
    void reportSeparatesReadingFromArithmetic() throws Exception {
        Signup owner = signupOwner("report@example.com", "동사수학학원");

        Map<String, String> pins = Map.of("문해력약한아이", "1111", "계산약한아이", "2222");
        List<Long> ids = new ArrayList<>();
        for (String name : List.of("문해력약한아이", "계산약한아이")) {
            ids.add(bodyOf(call(post("/api/students"), owner.token(),
                    Map.of("name", name, "grade", 3, "pin", pins.get(name))))
                    .get("student").get("id").asLong());
        }

        JsonNode created = bodyOf(call(post("/api/assignments"), owner.token(), new HashMap<>(Map.of(
                "title", "3학년 덧뺄셈", "grade", 3, "categories", CATEGORIES, "count", COUNT,
                "sameForEveryone", true, "studentIds", ids))));
        long assignmentId = created.get("assignment").get("id").asLong();
        String code = created.get("assignment").get("code").asText();

        JsonNode key = bodyOf(call(post("/api/worksheets"), owner.token(), new HashMap<>(Map.of(
                "grade", 3, "categories", CATEGORIES, "count", COUNT, "carry", true, "code", code))))
                .get("problems");

        // 두 아이 모두 딱 한 단계씩만 틀린다 — 총점은 같고, 틀린 자리만 다르다
        solveAs(owner, "문해력약한아이", "1111", key, false, true, true, true);   // 단서를 못 찾는다
        solveAs(owner, "계산약한아이", "2222", key, true, true, true, false);    // 계산에서 틀린다

        JsonNode report = bodyOf(call(get("/api/assignments/" + assignmentId + "/report"), owner.token(), null));

        assertThat(report.get("totalStudents").asInt()).isEqualTo(2);
        assertThat(report.get("completedStudents").asInt()).isEqualTo(2);

        Map<String, Integer> byStep = new HashMap<>();
        report.get("bySteps").forEach(r -> byStep.put(r.get("key").asText(), r.get("percent").asInt()));
        assertThat(byStep.get("CUE")).as("한 명만 단서를 맞혔다").isEqualTo(50);
        assertThat(byStep.get("OPERATION")).as("둘 다 연산은 골랐다").isEqualTo(100);
        assertThat(byStep.get("EXPRESSION")).as("둘 다 식은 세웠다").isEqualTo(100);
        assertThat(byStep.get("ANSWER")).as("한 명만 답을 맞혔다").isEqualTo(50);

        // 학생별로 어디가 약한지
        Map<String, JsonNode> rows = new HashMap<>();
        report.get("students").forEach(s -> rows.put(s.get("studentName").asText(), s));

        assertThat(stepPercent(rows.get("문해력약한아이"), "CUE")).isZero();
        assertThat(stepPercent(rows.get("문해력약한아이"), "ANSWER")).isEqualTo(100);

        assertThat(stepPercent(rows.get("계산약한아이"), "CUE")).isEqualTo(100);
        assertThat(stepPercent(rows.get("계산약한아이"), "ANSWER")).isZero();

        // 두 아이의 총점은 75% 로 똑같다. 총점만 보면 구분이 안 되고, 단계별로 봐야 갈린다.
        assertThat(rows.get("문해력약한아이").get("percent").asInt()).isEqualTo(75);
        assertThat(rows.get("계산약한아이").get("percent").asInt()).isEqualTo(75);

        assertThat(report.get("advice").asText()).isNotBlank();
        assertThat(report.get("byCategories").size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("아직 아무도 안 풀었으면 0% 로 나오고 안내 문구가 붙는다")
    void emptyReport() throws Exception {
        Signup owner = signupOwner("empty@example.com", "학원");
        long studentId = bodyOf(call(post("/api/students"), owner.token(),
                Map.of("name", "김민우", "grade", 3, "pin", "1234"))).get("student").get("id").asLong();

        long assignmentId = bodyOf(call(post("/api/assignments"), owner.token(), new HashMap<>(Map.of(
                "title", "아직 아무도", "grade", 3, "categories", CATEGORIES, "count", 3,
                "studentIds", List.of(studentId))))).get("assignment").get("id").asLong();

        JsonNode report = bodyOf(call(get("/api/assignments/" + assignmentId + "/report"), owner.token(), null));
        assertThat(report.get("completedStudents").asInt()).isZero();
        assertThat(report.get("bySteps")).hasSize(4);
        report.get("bySteps").forEach(r -> assertThat(r.get("percent").asInt()).isZero());
        assertThat(report.get("advice").asText()).contains("아직");
    }

    @Test
    @DisplayName("선생님 화면에서 누가 어디까지 풀었는지 보인다")
    void progressShowsUpInDetail() throws Exception {
        Signup owner = signupOwner("prog@example.com", "학원");
        long studentId = bodyOf(call(post("/api/students"), owner.token(),
                Map.of("name", "김민우", "grade", 3, "pin", "1234"))).get("student").get("id").asLong();

        JsonNode created = bodyOf(call(post("/api/assignments"), owner.token(), new HashMap<>(Map.of(
                "title", "진행 확인", "grade", 3, "categories", CATEGORIES, "count", 3,
                "sameForEveryone", true, "studentIds", List.of(studentId)))));
        long assignmentId = created.get("assignment").get("id").asLong();

        assertThat(created.get("students").get(0).get("status").asText()).isEqualTo("NOT_STARTED");
        assertThat(created.get("students").get(0).get("total").asInt()).isEqualTo(3 * 4);

        String token = bodyOf(call(post("/api/auth/student/login"), null,
                Map.of("academyCode", owner.academyCode(), "name", "김민우", "pin", "1234")))
                .get("token").asText();
        long said = bodyOf(call(get("/api/my/assignments"), token, null)).get(0).get("id").asLong();
        call(post("/api/my/assignments/" + said + "/answers"), token,
                Map.of("problemNo", 1, "step", "CUE", "value", "모두"));

        JsonNode detail = bodyOf(call(get("/api/assignments/" + assignmentId), owner.token(), null));
        assertThat(detail.get("students").get(0).get("status").asText()).isEqualTo("IN_PROGRESS");
        assertThat(detail.get("students").get(0).get("submitted").asInt()).isEqualTo(1);
    }

    // ── 도우미 ──

    private int stepPercent(JsonNode studentRow, String step) {
        for (JsonNode r : studentRow.get("bySteps")) {
            if (r.get("key").asText().equals(step)) {
                return r.get("percent").asInt();
            }
        }
        throw new IllegalStateException(step + " 없음");
    }

    /** 단계마다 일부러 맞히거나 틀리면서 과제를 끝까지 푼다. */
    private void solveAs(Signup owner, String name, String pin, JsonNode key,
                         boolean cue, boolean operation, boolean expression, boolean answer) throws Exception {
        String token = bodyOf(call(post("/api/auth/student/login"), null,
                Map.of("academyCode", owner.academyCode(), "name", name, "pin", pin))).get("token").asText();
        long said = bodyOf(call(get("/api/my/assignments"), token, null)).get(0).get("id").asLong();

        for (int i = 0; i < key.size(); i++) {
            JsonNode p = key.get(i);
            int no = i + 1;
            List<Integer> numbers = new ArrayList<>();
            p.get("numbers").forEach(n -> numbers.add(n.asInt()));

            send(token, said, no, "CUE", cue ? p.get("cues").get(0).asText() : "틀린단어", null);
            send(token, said, no, "OPERATION",
                    operation ? p.get("operation").asText() : flip(p.get("operation").asText()), null);

            List<Integer> wrongNumbers = new ArrayList<>(numbers);
            wrongNumbers.set(0, numbers.get(0) + 1);
            send(token, said, no, "EXPRESSION", null, expression ? numbers : wrongNumbers);

            send(token, said, no, "ANSWER",
                    answer ? String.valueOf(p.get("answer").asInt()) : String.valueOf(p.get("answer").asInt() + 1),
                    null);
        }
    }

    private String flip(String sign) {
        return sign.equals("+") ? "−" : "+";
    }

    private void send(String token, long said, int no, String step, String value, List<Integer> numbers)
            throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("problemNo", no);
        body.put("step", step);
        if (value != null) body.put("value", value);
        if (numbers != null) body.put("numbers", numbers);
        call(post("/api/my/assignments/" + said + "/answers"), token, body);
    }
}
