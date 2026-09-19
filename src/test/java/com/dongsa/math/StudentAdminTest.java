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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@DisplayName("원장이 학생을 관리한다")
class StudentAdminTest extends ApiTestSupport {

    private static final List<String> CATEGORIES = List.of("MERGE", "DECREASE");

    /** 학생 하나를 만들고 과제를 끝까지 풀린 뒤, 그 학생 id 를 돌려준다. */
    private long studentWhoSolved(Signup owner, String name, String pin,
                                  boolean cue, boolean answer) throws Exception {
        long studentId = bodyOf(call(post("/api/students"), owner.token(),
                Map.of("name", name, "grade", 3, "pin", pin))).get("student").get("id").asLong();

        JsonNode created = bodyOf(call(post("/api/assignments"), owner.token(), new HashMap<>(Map.of(
                "title", name + " 숙제", "grade", 3, "categories", CATEGORIES, "count", 2,
                "sameForEveryone", true, "studentIds", List.of(studentId)))));
        String code = created.get("assignment").get("code").asText();

        JsonNode key = bodyOf(call(post("/api/worksheets"), owner.token(), new HashMap<>(Map.of(
                "grade", 3, "categories", CATEGORIES, "count", 2, "carry", true, "code", code))))
                .get("problems");

        String token = bodyOf(call(post("/api/auth/student/login"), null,
                Map.of("academyCode", owner.academyCode(), "name", name, "pin", pin))).get("token").asText();
        long said = bodyOf(call(get("/api/my/assignments"), token, null)).get(0).get("id").asLong();

        for (int i = 0; i < key.size(); i++) {
            JsonNode p = key.get(i);
            int no = i + 1;
            List<Integer> numbers = new ArrayList<>();
            p.get("numbers").forEach(n -> numbers.add(n.asInt()));
            send(token, said, no, "CUE", cue ? p.get("cues").get(0).asText() : "틀린단어", null);
            send(token, said, no, "OPERATION", p.get("operation").asText(), null);
            send(token, said, no, "EXPRESSION", null, numbers);
            send(token, said, no, "ANSWER",
                    String.valueOf(answer ? p.get("answer").asInt() : p.get("answer").asInt() + 1), null);
        }
        return studentId;
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

    // ────────── 성적 ──────────

    @Test
    @DisplayName("한 아이의 누적 성적이 단계별·유형별·과제별로 나온다")
    void studentReport() throws Exception {
        Signup owner = signupOwner("rep1@example.com", "학원");
        long id = studentWhoSolved(owner, "김민우", "1111", false, true);   // 단서만 틀린다

        JsonNode report = bodyOf(call(get("/api/students/" + id + "/report"), owner.token(), null));

        assertThat(report.get("studentName").asText()).isEqualTo("김민우");
        assertThat(report.get("assignmentCount").asInt()).isEqualTo(1);
        assertThat(report.get("completedCount").asInt()).isEqualTo(1);
        assertThat(report.get("percent").asInt()).isEqualTo(75);

        Map<String, Integer> byStep = new HashMap<>();
        report.get("bySteps").forEach(r -> byStep.put(r.get("key").asText(), r.get("percent").asInt()));
        assertThat(byStep.get("CUE")).isZero();
        assertThat(byStep.get("ANSWER")).isEqualTo(100);

        assertThat(report.get("weakestStep").asText()).isEqualTo("CUE");
        assertThat(report.get("advice").asText()).contains("단서");
        assertThat(report.get("byCategories").size()).isGreaterThanOrEqualTo(1);
        assertThat(report.get("assignments")).hasSize(1);
        assertThat(report.get("assignments").get(0).get("percent").asInt()).isEqualTo(75);
    }

    @Test
    @DisplayName("여러 과제를 풀면 누적해서 합쳐진다")
    void reportAddsUpAcrossAssignments() throws Exception {
        Signup owner = signupOwner("rep2@example.com", "학원");
        long id = studentWhoSolved(owner, "박서윤", "2222", true, true);

        // 같은 아이에게 과제를 하나 더 내고 그대로 둔다 (안 푼 과제)
        call(post("/api/assignments"), owner.token(), new HashMap<>(Map.of(
                "title", "두 번째", "grade", 3, "categories", CATEGORIES, "count", 2,
                "studentIds", List.of(id))));

        JsonNode report = bodyOf(call(get("/api/students/" + id + "/report"), owner.token(), null));
        assertThat(report.get("assignmentCount").asInt()).isEqualTo(2);
        assertThat(report.get("completedCount").asInt()).isEqualTo(1);
        assertThat(report.get("assignments")).hasSize(2);

        // 안 푼 과제는 0점으로 목록에 남는다 — 안 한 것도 정보다
        List<Integer> percents = new ArrayList<>();
        report.get("assignments").forEach(a -> percents.add(a.get("percent").asInt()));
        assertThat(percents).contains(0, 100);
    }

    @Test
    @DisplayName("학원 학생 전체 목록에서 약한 아이가 위로 온다")
    void academyOverviewSortsWeakestFirst() throws Exception {
        Signup owner = signupOwner("rep3@example.com", "학원");
        studentWhoSolved(owner, "잘하는아이", "1111", true, true);     // 100%
        studentWhoSolved(owner, "약한아이", "2222", false, false);      // 50%
        call(post("/api/students"), owner.token(), Map.of("name", "아직안푼아이", "grade", 3, "pin", "3333"));

        JsonNode rows = bodyOf(call(get("/api/students/report"), owner.token(), null));
        assertThat(rows).hasSize(3);

        List<String> order = new ArrayList<>();
        rows.forEach(r -> order.add(r.get("studentName").asText()));
        assertThat(order).containsExactly("약한아이", "잘하는아이", "아직안푼아이");

        assertThat(rows.get(0).get("percent").asInt()).isEqualTo(50);
        assertThat(rows.get(1).get("percent").asInt()).isEqualTo(100);
        assertThat(rows.get(2).get("answeredSteps").asInt()).as("안 푼 아이는 맨 아래").isZero();
    }

    @Test
    @DisplayName("다른 학원 학생의 성적은 볼 수 없다")
    void cannotSeeOtherAcademyReport() throws Exception {
        Signup a = signupOwner("repa@example.com", "가학원");
        Signup b = signupOwner("repb@example.com", "나학원");
        long id = studentWhoSolved(a, "김민우", "1111", true, true);

        assertThat(call(get("/api/students/" + id + "/report"), b.token(), null)
                .getResponse().getStatus()).isEqualTo(404);
        assertThat(bodyOf(call(get("/api/students/report"), b.token(), null))).isEmpty();
    }

    // ────────── 삭제 ──────────

    @Test
    @DisplayName("원장은 학생을 완전히 지울 수 있고, 성적도 같이 사라진다")
    void ownerCanDeleteStudent() throws Exception {
        Signup owner = signupOwner("del1@example.com", "학원");
        long id = studentWhoSolved(owner, "그만둔아이", "1111", true, true);

        MvcResult r = call(delete("/api/students/" + id), owner.token(), null);
        assertThat(r.getResponse().getStatus()).isEqualTo(200);

        JsonNode removed = bodyOf(r);
        assertThat(removed.get("name").asText()).isEqualTo("그만둔아이");
        assertThat(removed.get("removedAssignments").asInt()).isEqualTo(1);
        assertThat(removed.get("removedSubmissions").asInt()).as("2문제 × 4단계").isEqualTo(8);

        assertThat(bodyOf(call(get("/api/students"), owner.token(), null))).isEmpty();
        assertThat(call(get("/api/students/" + id + "/report"), owner.token(), null)
                .getResponse().getStatus()).isEqualTo(404);
    }

    @Test
    @DisplayName("지워진 아이는 다시 로그인할 수 없다")
    void deletedStudentCannotLogIn() throws Exception {
        Signup owner = signupOwner("del2@example.com", "학원");
        long id = bodyOf(call(post("/api/students"), owner.token(),
                Map.of("name", "김민우", "grade", 3, "pin", "1234"))).get("student").get("id").asLong();

        assertThat(call(post("/api/auth/student/login"), null,
                Map.of("academyCode", owner.academyCode(), "name", "김민우", "pin", "1234"))
                .getResponse().getStatus()).isEqualTo(200);

        call(delete("/api/students/" + id), owner.token(), null);

        assertThat(call(post("/api/auth/student/login"), null,
                Map.of("academyCode", owner.academyCode(), "name", "김민우", "pin", "1234"))
                .getResponse().getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("강사는 학생을 지울 수 없다 — 원장만 된다")
    void teacherCannotDelete() throws Exception {
        Signup owner = signupOwner("del3@example.com", "학원");
        long id = bodyOf(call(post("/api/students"), owner.token(),
                Map.of("name", "김민우", "grade", 3, "pin", "1234"))).get("student").get("id").asLong();

        call(post("/api/academy/teachers"), owner.token(),
                Map.of("email", "t@example.com", "password", "password123", "name", "강사"));
        String teacherToken = bodyOf(call(post("/api/auth/login"), null,
                Map.of("email", "t@example.com", "password", "password123"))).get("token").asText();

        MvcResult r = call(delete("/api/students/" + id), teacherToken, null);
        assertThat(r.getResponse().getStatus()).isEqualTo(403);
        assertThat(bodyOf(r).get("code").asText()).isEqualTo("OWNER_ONLY");

        // 강사도 성적은 볼 수 있다
        assertThat(call(get("/api/students/report"), teacherToken, null)
                .getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("다른 학원 학생은 지울 수 없다")
    void cannotDeleteOtherAcademyStudent() throws Exception {
        Signup a = signupOwner("dela@example.com", "가학원");
        Signup b = signupOwner("delb@example.com", "나학원");
        long id = bodyOf(call(post("/api/students"), a.token(),
                Map.of("name", "김민우", "grade", 3, "pin", "1234"))).get("student").get("id").asLong();

        assertThat(call(delete("/api/students/" + id), b.token(), null)
                .getResponse().getStatus()).isEqualTo(404);
        assertThat(bodyOf(call(get("/api/students"), a.token(), null))).hasSize(1);
    }

    @Test
    @DisplayName("지워도 그 과제 자체와 다른 아이의 기록은 남는다")
    void deletingOneStudentKeepsTheRest() throws Exception {
        Signup owner = signupOwner("del4@example.com", "학원");
        long keep = studentWhoSolved(owner, "남는아이", "1111", true, true);
        long drop = studentWhoSolved(owner, "지울아이", "2222", true, true);

        call(delete("/api/students/" + drop), owner.token(), null);

        assertThat(bodyOf(call(get("/api/students"), owner.token(), null))).hasSize(1);
        assertThat(bodyOf(call(get("/api/assignments"), owner.token(), null)))
                .as("과제는 두 건 다 남는다").hasSize(2);
        assertThat(bodyOf(call(get("/api/students/" + keep + "/report"), owner.token(), null))
                .get("percent").asInt()).isEqualTo(100);
    }
}
