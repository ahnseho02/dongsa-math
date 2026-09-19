package com.dongsa.math;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@DisplayName("과제 내주기와 단계별 채점")
class AssignmentFlowTest extends ApiTestSupport {

    private static final List<String> CATEGORIES = List.of("MERGE", "DECREASE");
    private static final int COUNT = 3;

    /** 선생님이 과제를 내고, 학생 하나가 로그인한 상태를 만든다. */
    private record Scene(Signup owner, long assignmentId, String assignmentCode,
                         long studentAssignmentId, String studentToken) {}

    private Scene setUp(String email, boolean sameForEveryone) throws Exception {
        Signup owner = signupOwner(email, "동사수학학원");

        long studentId = bodyOf(call(post("/api/students"), owner.token(),
                Map.of("name", "김민우", "grade", 3, "pin", "1234")))
                .get("student").get("id").asLong();

        Map<String, Object> req = new HashMap<>(Map.of(
                "title", "3학년 덧셈 숙제", "grade", 3, "categories", CATEGORIES,
                "count", COUNT, "carry", true, "sameForEveryone", sameForEveryone,
                "studentIds", List.of(studentId)));

        JsonNode created = bodyOf(call(post("/api/assignments"), owner.token(), req));
        long assignmentId = created.get("assignment").get("id").asLong();
        String code = created.get("assignment").get("code").asText();

        String studentToken = bodyOf(call(post("/api/auth/student/login"), null,
                Map.of("academyCode", owner.academyCode(), "name", "김민우", "pin", "1234")))
                .get("token").asText();

        long studentAssignmentId = bodyOf(call(get("/api/my/assignments"), studentToken, null))
                .get(0).get("id").asLong();

        return new Scene(owner, assignmentId, code, studentAssignmentId, studentToken);
    }

    /** 선생님 화면으로 같은 학습지를 뽑아 정답을 알아낸다 (모두 같은 문제일 때만 쓸 수 있다). */
    private JsonNode answerKey(Scene scene) throws Exception {
        Map<String, Object> req = new HashMap<>(Map.of(
                "grade", 3, "categories", CATEGORIES, "count", COUNT,
                "carry", true, "code", scene.assignmentCode()));
        return bodyOf(call(post("/api/worksheets"), scene.owner().token(), req)).get("problems");
    }

    private JsonNode answer(Scene scene, int problemNo, String step, Object value, List<Integer> numbers)
            throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("problemNo", problemNo);
        body.put("step", step);
        if (value != null) body.put("value", String.valueOf(value));
        if (numbers != null) body.put("numbers", numbers);
        return bodyOf(call(post("/api/my/assignments/" + scene.studentAssignmentId() + "/answers"),
                scene.studentToken(), body));
    }

    // ────────────────────────────────────────────

    @Test
    @DisplayName("아이가 받는 문제에는 정답도 단서도 연산도 없다")
    void studentNeverSeesTheAnswer() throws Exception {
        Scene scene = setUp("hide@example.com", true);

        MvcResult r = call(get("/api/my/assignments/" + scene.studentAssignmentId()), scene.studentToken(), null);
        String raw = r.getResponse().getContentAsString();
        JsonNode problems = bodyOf(r).get("problems");

        assertThat(problems).hasSize(COUNT);
        JsonNode first = problems.get(0);
        assertThat(first.has("answer")).as("정답이 응답에 들어 있으면 안 된다").isFalse();
        assertThat(first.has("cues")).as("단서가 응답에 들어 있으면 안 된다").isFalse();
        assertThat(first.has("operation")).as("연산이 응답에 들어 있으면 안 된다").isFalse();
        assertThat(first.has("expression")).isFalse();

        // 풀기에 필요한 것은 들어 있다
        assertThat(first.get("sentence").asText()).isNotBlank();
        assertThat(first.get("tappableWords").size()).isGreaterThanOrEqualTo(2);
        assertThat(first.get("numbers").size()).isGreaterThanOrEqualTo(2);

        // 아직 아무 단계도 안 냈으므로 정답 칸이 전부 비어 있다
        for (JsonNode step : first.get("steps")) {
            assertThat(step.get("submitted").asBoolean()).isFalse();
            assertThat(step.get("correctValue").isNull()).isTrue();
        }
        assertThat(raw).doesNotContain("correctValue\":\"");
    }

    @Test
    @DisplayName("네 단계를 다 맞히면 문제가 끝나고, 다 풀면 과제가 끝난다")
    void solveEverythingCorrectly() throws Exception {
        Scene scene = setUp("solve@example.com", true);
        JsonNode key = answerKey(scene);

        JsonNode last = null;
        for (int i = 0; i < COUNT; i++) {
            JsonNode p = key.get(i);
            int no = i + 1;
            List<Integer> numbers = new ArrayList<>();
            p.get("numbers").forEach(n -> numbers.add(n.asInt()));

            assertThat(answer(scene, no, "CUE", p.get("cues").get(0).asText(), null)
                    .get("correct").asBoolean()).isTrue();
            assertThat(answer(scene, no, "OPERATION", p.get("operation").asText(), null)
                    .get("correct").asBoolean()).isTrue();
            assertThat(answer(scene, no, "EXPRESSION", null, numbers)
                    .get("correct").asBoolean()).isTrue();
            last = answer(scene, no, "ANSWER", p.get("answer").asInt(), null);
            assertThat(last.get("correct").asBoolean()).isTrue();
            assertThat(last.get("problemDone").asBoolean()).isTrue();
        }

        assertThat(last.get("assignmentDone").asBoolean()).isTrue();
        assertThat(last.get("correctSteps").asInt()).isEqualTo(COUNT * 4);
        assertThat(bodyOf(call(get("/api/my/assignments"), scene.studentToken(), null))
                .get(0).get("status").asText()).isEqualTo("DONE");
    }

    @Test
    @DisplayName("틀리면 그제서야 정답을 알려 준다")
    void wrongAnswerRevealsTheCorrectOne() throws Exception {
        Scene scene = setUp("wrong@example.com", true);
        JsonNode key = answerKey(scene);

        JsonNode r = answer(scene, 1, "CUE", "있습니다만틀린단어", null);
        assertThat(r.get("correct").asBoolean()).isFalse();
        assertThat(r.get("correctValue").asText()).isEqualTo(String.join(", ",
                toList(key.get(0).get("cues"))));
        assertThat(r.get("nextStep").asText()).isEqualTo("OPERATION");
    }

    @Test
    @DisplayName("연산 기호는 −, -, x, * 무엇으로 보내도 알아듣는다")
    void operationSymbolsAreForgiving() throws Exception {
        Scene scene = setUp("sign@example.com", true);
        JsonNode key = answerKey(scene);

        int no = -1;
        for (int i = 0; i < COUNT; i++) {
            if (key.get(i).get("operation").asText().equals("−")) { no = i + 1; break; }
        }
        if (no < 0) return;   // 이 시드에 뺄셈이 없으면 볼 것이 없다

        answer(scene, no, "CUE", key.get(no - 1).get("cues").get(0).asText(), null);
        assertThat(answer(scene, no, "OPERATION", "-", null).get("correct").asBoolean())
                .as("보통 하이픈으로 보내도 맞아야 한다").isTrue();
    }

    @Test
    @DisplayName("덧셈은 숫자 순서를 바꿔 써도 맞다 — 뺄셈은 아니다")
    void expressionOrderRules() throws Exception {
        Scene scene = setUp("order@example.com", true);
        JsonNode key = answerKey(scene);

        for (int i = 0; i < COUNT; i++) {
            JsonNode p = key.get(i);
            int no = i + 1;
            List<Integer> reversed = new ArrayList<>(toIntList(p.get("numbers")));
            java.util.Collections.reverse(reversed);
            if (reversed.equals(toIntList(p.get("numbers")))) continue;   // 두 수가 같으면 못 본다

            answer(scene, no, "CUE", p.get("cues").get(0).asText(), null);
            answer(scene, no, "OPERATION", p.get("operation").asText(), null);
            boolean correct = answer(scene, no, "EXPRESSION", null, reversed).get("correct").asBoolean();

            if (p.get("operation").asText().equals("+")) {
                assertThat(correct).as("덧셈은 순서를 따지지 않는다").isTrue();
            } else {
                assertThat(correct).as("뺄셈은 순서가 답을 바꾼다").isFalse();
            }
        }
    }

    @Test
    @DisplayName("앞 단계를 건너뛸 수 없다")
    void stepsMustBeInOrder() throws Exception {
        Scene scene = setUp("order2@example.com", true);

        MvcResult r = call(post("/api/my/assignments/" + scene.studentAssignmentId() + "/answers"),
                scene.studentToken(), Map.of("problemNo", 1, "step", "ANSWER", "value", "100"));

        assertThat(r.getResponse().getStatus()).isEqualTo(409);
        assertThat(bodyOf(r).get("code").asText()).isEqualTo("STEP_OUT_OF_ORDER");
    }

    @Test
    @DisplayName("같은 단계를 두 번 보내도 처음 결과가 그대로다 — 폰은 잘 끊긴다")
    void resubmittingIsIdempotent() throws Exception {
        Scene scene = setUp("retry@example.com", true);
        JsonNode key = answerKey(scene);

        JsonNode wrong = answer(scene, 1, "CUE", "엉뚱한단어", null);
        assertThat(wrong.get("correct").asBoolean()).isFalse();
        assertThat(wrong.get("answeredSteps").asInt()).isEqualTo(1);

        // 이번엔 정답을 보내도 이미 낸 단계라 결과가 바뀌지 않는다
        JsonNode again = answer(scene, 1, "CUE", key.get(0).get("cues").get(0).asText(), null);
        assertThat(again.get("correct").asBoolean()).as("점수를 다시 딸 수 없다").isFalse();
        assertThat(again.get("answeredSteps").asInt()).as("제출 수가 늘면 안 된다").isEqualTo(1);
    }

    @Test
    @DisplayName("마감된 과제에는 답을 낼 수 없다")
    void closedAssignmentRejectsAnswers() throws Exception {
        Scene scene = setUp("closed@example.com", true);
        call(patch("/api/assignments/" + scene.assignmentId() + "/closed"),
                scene.owner().token(), Map.of("closed", true));

        MvcResult r = call(post("/api/my/assignments/" + scene.studentAssignmentId() + "/answers"),
                scene.studentToken(), Map.of("problemNo", 1, "step", "CUE", "value", "모두"));

        assertThat(r.getResponse().getStatus()).isEqualTo(409);
        assertThat(bodyOf(r).get("code").asText()).isEqualTo("ASSIGNMENT_CLOSED");
    }

    @Test
    @DisplayName("마감 시각이 지나도 받지 않는다")
    void pastDueRejectsAnswers() throws Exception {
        Signup owner = signupOwner("due@example.com", "학원");
        long studentId = bodyOf(call(post("/api/students"), owner.token(),
                Map.of("name", "김민우", "grade", 3, "pin", "1234"))).get("student").get("id").asLong();

        Map<String, Object> req = new HashMap<>(Map.of(
                "title", "어제까지", "grade", 3, "categories", CATEGORIES, "count", 2,
                "studentIds", List.of(studentId)));
        req.put("dueAt", Instant.now().minusSeconds(3600).toString());
        call(post("/api/assignments"), owner.token(), req);

        String studentToken = bodyOf(call(post("/api/auth/student/login"), null,
                Map.of("academyCode", owner.academyCode(), "name", "김민우", "pin", "1234")))
                .get("token").asText();
        long said = bodyOf(call(get("/api/my/assignments"), studentToken, null)).get(0).get("id").asLong();

        MvcResult r = call(post("/api/my/assignments/" + said + "/answers"), studentToken,
                Map.of("problemNo", 1, "step", "CUE", "value", "모두"));
        assertThat(r.getResponse().getStatus()).isEqualTo(409);
    }

    @Test
    @DisplayName("남의 과제는 열 수 없다")
    void cannotOpenSomeoneElsesAssignment() throws Exception {
        Scene a = setUp("mine@example.com", true);
        Scene b = setUp("theirs@example.com", true);

        MvcResult r = call(get("/api/my/assignments/" + a.studentAssignmentId()), b.studentToken(), null);
        assertThat(r.getResponse().getStatus()).isEqualTo(404);
    }

    @Test
    @DisplayName("기본은 아이마다 다른 문제 — 옆자리를 봐도 소용없다")
    void differentProblemsPerStudentByDefault() throws Exception {
        Signup owner = signupOwner("vary@example.com", "학원");
        List<String> tokens = new ArrayList<>();
        List<Long> ids = new ArrayList<>();
        for (String name : List.of("김민우", "박서윤")) {
            ids.add(bodyOf(call(post("/api/students"), owner.token(),
                    Map.of("name", name, "grade", 3, "pin", name.equals("김민우") ? "1111" : "2222")))
                    .get("student").get("id").asLong());
        }
        call(post("/api/assignments"), owner.token(), new HashMap<>(Map.of(
                "title", "각자 다른 문제", "grade", 3, "categories", CATEGORIES, "count", 5,
                "sameForEveryone", false, "studentIds", ids)));

        for (String[] who : new String[][]{{"김민우", "1111"}, {"박서윤", "2222"}}) {
            tokens.add(bodyOf(call(post("/api/auth/student/login"), null,
                    Map.of("academyCode", owner.academyCode(), "name", who[0], "pin", who[1])))
                    .get("token").asText());
        }

        List<String> first = sentencesOf(tokens.get(0));
        List<String> second = sentencesOf(tokens.get(1));
        assertThat(first).hasSize(5);
        assertThat(first).as("아이마다 문제가 달라야 한다").isNotEqualTo(second);
    }

    @Test
    @DisplayName("sameForEveryone 을 켜면 반 전체가 같은 문제를 받는다")
    void sameProblemsWhenAsked() throws Exception {
        Signup owner = signupOwner("same@example.com", "학원");
        List<Long> ids = new ArrayList<>();
        for (String[] who : new String[][]{{"김민우", "1111"}, {"박서윤", "2222"}}) {
            ids.add(bodyOf(call(post("/api/students"), owner.token(),
                    Map.of("name", who[0], "grade", 3, "pin", who[1]))).get("student").get("id").asLong());
        }
        call(post("/api/assignments"), owner.token(), new HashMap<>(Map.of(
                "title", "같은 시험지", "grade", 3, "categories", CATEGORIES, "count", 5,
                "sameForEveryone", true, "studentIds", ids)));

        List<String> all = new ArrayList<>();
        for (String[] who : new String[][]{{"김민우", "1111"}, {"박서윤", "2222"}}) {
            String token = bodyOf(call(post("/api/auth/student/login"), null,
                    Map.of("academyCode", owner.academyCode(), "name", who[0], "pin", who[1])))
                    .get("token").asText();
            all.add(String.join("|", sentencesOf(token)));
        }
        assertThat(all.get(0)).isEqualTo(all.get(1));
    }

    // ── 도우미 ──

    private List<String> sentencesOf(String studentToken) throws Exception {
        long id = bodyOf(call(get("/api/my/assignments"), studentToken, null)).get(0).get("id").asLong();
        JsonNode problems = bodyOf(call(get("/api/my/assignments/" + id), studentToken, null)).get("problems");
        List<String> out = new ArrayList<>();
        problems.forEach(p -> out.add(p.get("sentence").asText()));
        return out;
    }

    private List<String> toList(JsonNode array) {
        List<String> out = new ArrayList<>();
        array.forEach(n -> out.add(n.asText()));
        return out;
    }

    private List<Integer> toIntList(JsonNode array) {
        List<Integer> out = new ArrayList<>();
        array.forEach(n -> out.add(n.asInt()));
        return out;
    }
}
