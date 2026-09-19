package com.dongsa.math;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@DisplayName("연산 게임")
class GameTest extends ApiTestSupport {

    private String studentToken(Signup owner, String name, String pin, int grade) throws Exception {
        call(post("/api/students"), owner.token(), Map.of("name", name, "grade", grade, "pin", pin));
        return bodyOf(call(post("/api/auth/student/login"), null,
                Map.of("academyCode", owner.academyCode(), "name", name, "pin", pin))).get("token").asText();
    }

    /** 문제 식을 직접 계산해서 답을 만든다 — 서버가 준 것이 아니라 우리가 푼 것이다. */
    private int solve(String expression) {
        String[] parts = expression.split(" ");
        int a = Integer.parseInt(parts[0]);
        int b = Integer.parseInt(parts[2]);
        return switch (parts[1]) {
            case "+" -> a + b;
            case "−" -> a - b;
            case "×" -> a * b;
            case "÷" -> a / b;
            default -> throw new IllegalStateException("모르는 기호: " + parts[1]);
        };
    }

    private JsonNode play(String token, int howManyRight, int howManyWrong) throws Exception {
        JsonNode start = bodyOf(call(post("/api/my/games"), token, null));
        long roundId = start.get("roundId").asLong();

        List<Map<String, Object>> answers = new ArrayList<>();
        JsonNode problems = start.get("problems");
        for (int i = 0; i < howManyRight; i++) {
            JsonNode p = problems.get(i);
            answers.add(Map.of("no", p.get("no").asInt(), "value", String.valueOf(solve(p.get("expression").asText()))));
        }
        for (int i = howManyRight; i < howManyRight + howManyWrong; i++) {
            JsonNode p = problems.get(i);
            answers.add(Map.of("no", p.get("no").asInt(), "value", String.valueOf(solve(p.get("expression").asText()) + 1)));
        }
        return bodyOf(call(post("/api/my/games/" + roundId + "/finish"), token,
                Map.of("answers", answers)));
    }

    @Test
    @DisplayName("게임을 시작하면 문제만 오고 정답은 오지 않는다")
    void startGivesNoAnswers() throws Exception {
        Signup owner = signupOwner("g1@example.com", "학원");
        String token = studentToken(owner, "김민우", "1111", 3);

        MvcResult r = call(post("/api/my/games"), token, null);
        assertThat(r.getResponse().getStatus()).isEqualTo(201);
        String raw = r.getResponse().getContentAsString();
        JsonNode body = bodyOf(r);

        assertThat(body.get("limitSeconds").asInt()).isEqualTo(60);
        assertThat(body.get("problems")).hasSize(40);
        assertThat(raw).doesNotContain("\"answer\"");

        JsonNode first = body.get("problems").get(0);
        assertThat(first.has("answer")).isFalse();
        assertThat(first.get("expression").asText()).matches("\\d+ [+−×÷] \\d+");
    }

    @Test
    @DisplayName("맞힌 개수만 점수가 되고, 틀린 것만 정답을 알려 준다")
    void scoringCountsOnlyCorrect() throws Exception {
        Signup owner = signupOwner("g2@example.com", "학원");
        String token = studentToken(owner, "김민우", "1111", 3);

        JsonNode result = play(token, 7, 3);

        assertThat(result.get("solved").asInt()).isEqualTo(7);
        assertThat(result.get("attempted").asInt()).isEqualTo(10);
        assertThat(result.get("problemCount").asInt()).isEqualTo(40);
        assertThat(result.get("wrong")).hasSize(3);
        assertThat(result.get("wrong").get(0).get("answer").asInt())
                .isEqualTo(solve(result.get("wrong").get(0).get("expression").asText()));
        assertThat(result.get("newBest").asBoolean()).isTrue();
        assertThat(result.get("rank").asInt()).isEqualTo(1);
    }

    @Test
    @DisplayName("한 판은 한 번만 제출할 수 있다")
    void cannotFinishTwice() throws Exception {
        Signup owner = signupOwner("g3@example.com", "학원");
        String token = studentToken(owner, "김민우", "1111", 3);

        JsonNode start = bodyOf(call(post("/api/my/games"), token, null));
        long roundId = start.get("roundId").asLong();
        Map<String, Object> body = Map.of("answers", List.of());

        assertThat(call(post("/api/my/games/" + roundId + "/finish"), token, body)
                .getResponse().getStatus()).isEqualTo(200);

        MvcResult again = call(post("/api/my/games/" + roundId + "/finish"), token, body);
        assertThat(again.getResponse().getStatus()).isEqualTo(409);
        assertThat(bodyOf(again).get("code").asText()).isEqualTo("GAME_ALREADY_FINISHED");
    }

    @Test
    @DisplayName("남의 게임은 제출할 수 없다")
    void cannotFinishSomeoneElsesGame() throws Exception {
        Signup owner = signupOwner("g4@example.com", "학원");
        String mine = studentToken(owner, "김민우", "1111", 3);
        String other = studentToken(owner, "박서윤", "2222", 3);

        long roundId = bodyOf(call(post("/api/my/games"), mine, null)).get("roundId").asLong();

        assertThat(call(post("/api/my/games/" + roundId + "/finish"), other,
                Map.of("answers", List.of())).getResponse().getStatus()).isEqualTo(404);
    }

    @Test
    @DisplayName("최고 기록만 순위에 올라간다 — 많이 한다고 유리하지 않다")
    void rankingUsesBestRound() throws Exception {
        Signup owner = signupOwner("g5@example.com", "학원");
        String a = studentToken(owner, "빠른아이", "1111", 3);
        String b = studentToken(owner, "느린아이", "2222", 3);

        play(a, 5, 0);
        play(a, 12, 0);   // 최고 기록
        play(a, 3, 0);
        play(b, 8, 0);

        JsonNode ranking = bodyOf(call(get("/api/my/games/ranking"), a, null));
        assertThat(ranking.get("players").asInt()).isEqualTo(2);
        assertThat(ranking.get("myBest").asInt()).isEqualTo(12);
        assertThat(ranking.get("myPlays").asInt()).isEqualTo(3);
        assertThat(ranking.get("myRank").asInt()).isEqualTo(1);

        JsonNode rows = ranking.get("rows");
        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).get("studentName").asText()).isEqualTo("빠른아이");
        assertThat(rows.get(0).get("best").asInt()).isEqualTo(12);
        assertThat(rows.get(0).get("me").asBoolean()).isTrue();
        assertThat(rows.get(1).get("studentName").asText()).isEqualTo("느린아이");
        assertThat(rows.get(1).get("me").asBoolean()).isFalse();
    }

    @Test
    @DisplayName("다른 학원 아이는 순위에 섞이지 않는다")
    void rankingIsPerAcademy() throws Exception {
        Signup a = signupOwner("g6a@example.com", "가학원");
        Signup b = signupOwner("g6b@example.com", "나학원");
        play(studentToken(a, "가학원아이", "1111", 3), 20, 0);
        String mine = studentToken(b, "나학원아이", "2222", 3);
        play(mine, 4, 0);

        JsonNode ranking = bodyOf(call(get("/api/my/games/ranking"), mine, null));
        assertThat(ranking.get("players").asInt()).isEqualTo(1);
        assertThat(ranking.get("rows")).hasSize(1);
        assertThat(ranking.get("rows").get(0).get("studentName").asText()).isEqualTo("나학원아이");
    }

    @Test
    @DisplayName("선생님도 순위를 볼 수 있다")
    void teacherSeesRanking() throws Exception {
        Signup owner = signupOwner("g7@example.com", "학원");
        play(studentToken(owner, "김민우", "1111", 3), 6, 0);

        JsonNode ranking = bodyOf(call(get("/api/games/ranking"), owner.token(), null));
        assertThat(ranking.get("rows")).hasSize(1);
        assertThat(ranking.get("myRank").isNull()).as("선생님은 순위에 없다").isTrue();
    }

    @Test
    @DisplayName("학년에 맞는 연산만 나온다 — 초1에 나눗셈은 없다")
    void operationsFitTheGrade() throws Exception {
        Signup owner = signupOwner("g8@example.com", "학원");

        JsonNode first = bodyOf(call(post("/api/my/games"), studentToken(owner, "초1아이", "1111", 1), null));
        List<String> signs = new ArrayList<>();
        first.get("problems").forEach(p -> signs.add(p.get("expression").asText().split(" ")[1]));
        assertThat(signs).containsAnyOf("+", "−").doesNotContain("×", "÷");

        JsonNode third = bodyOf(call(post("/api/my/games"), studentToken(owner, "초3아이", "2222", 3), null));
        List<String> signs3 = new ArrayList<>();
        third.get("problems").forEach(p -> signs3.add(p.get("expression").asText().split(" ")[1]));
        assertThat(signs3).contains("×").contains("÷");
    }

    @Test
    @DisplayName("답이 음수가 되거나 나머지가 남는 문제는 나오지 않는다")
    void problemsAreAlwaysSolvable() throws Exception {
        Signup owner = signupOwner("g9@example.com", "학원");
        for (int grade = 1; grade <= 6; grade++) {
            String token = studentToken(owner, "초" + grade + "아이", "111" + grade, grade);
            JsonNode start = bodyOf(call(post("/api/my/games"), token, null));
            for (JsonNode p : start.get("problems")) {
                String expression = p.get("expression").asText();
                String[] parts = expression.split(" ");
                int a = Integer.parseInt(parts[0]);
                int b = Integer.parseInt(parts[2]);
                if (parts[1].equals("−")) {
                    assertThat(a).as("초%d %s", grade, expression).isGreaterThan(b);
                }
                if (parts[1].equals("÷")) {
                    assertThat(a % b).as("초%d %s", grade, expression).isZero();
                }
                assertThat(solve(expression)).as("초%d %s", grade, expression).isNotNegative();
            }
        }
    }

    @Test
    @DisplayName("학생을 지우면 게임 기록도 같이 사라진다")
    void deletingStudentRemovesGames() throws Exception {
        Signup owner = signupOwner("g10@example.com", "학원");
        long id = bodyOf(call(post("/api/students"), owner.token(),
                Map.of("name", "그만둔아이", "grade", 3, "pin", "1111"))).get("student").get("id").asLong();
        String token = bodyOf(call(post("/api/auth/student/login"), null,
                Map.of("academyCode", owner.academyCode(), "name", "그만둔아이", "pin", "1111")))
                .get("token").asText();
        play(token, 5, 0);

        assertThat(bodyOf(call(get("/api/games/ranking"), owner.token(), null)).get("rows")).hasSize(1);

        call(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .delete("/api/students/" + id), owner.token(), null);

        assertThat(bodyOf(call(get("/api/games/ranking"), owner.token(), null)).get("rows")).isEmpty();
    }

    @Test
    @DisplayName("학생 토큰으로만 게임을 할 수 있다")
    void teacherCannotPlay() throws Exception {
        Signup owner = signupOwner("g11@example.com", "학원");
        assertThat(call(post("/api/my/games"), owner.token(), null).getResponse().getStatus())
                .isEqualTo(403);
    }
}
