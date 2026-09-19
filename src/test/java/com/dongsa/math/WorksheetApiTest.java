package com.dongsa.math;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@DisplayName("학습지 API")
class WorksheetApiTest extends ApiTestSupport {

    @Test
    @DisplayName("학년을 고르면 낼 수 있는 유형과 숫자 범위를 알려 준다")
    void options() throws Exception {
        Signup owner = signupOwner("w1@example.com", "학원");

        JsonNode g1 = bodyOf(call(get("/api/worksheets/options?grade=1"), owner.token(), null));
        assertThat(g1.get("numberRange").asText()).isEqualTo("한 자리 수");
        assertThat(g1.get("categories").toString()).doesNotContain("SHARE");

        JsonNode g3 = bodyOf(call(get("/api/worksheets/options?grade=3"), owner.token(), null));
        assertThat(g3.get("numberRange").asText()).isEqualTo("세 자리 수");
        assertThat(g3.get("categories").toString()).contains("SHARE");
    }

    @Test
    @DisplayName("학습지를 뽑으면 문장·식·정답이 함께 나온다")
    void generate() throws Exception {
        Signup owner = signupOwner("w2@example.com", "학원");

        MvcResult r = call(post("/api/worksheets"), owner.token(), Map.of(
                "grade", 3, "categories", List.of("MERGE", "DECREASE"), "count", 5, "carry", true));

        assertThat(r.getResponse().getStatus()).isEqualTo(200);
        JsonNode body = bodyOf(r);
        assertThat(body.get("code").asText()).hasSize(6);
        assertThat(body.get("problems")).hasSize(5);

        JsonNode first = body.get("problems").get(0);
        assertThat(first.get("sentence").asText()).isNotBlank().doesNotContain("{");
        assertThat(first.get("expression").asText()).matches("\\d+ [+−] \\d+");
        assertThat(first.get("tappableWords").size()).isGreaterThanOrEqualTo(2);
        assertThat(first.get("cues").size()).isGreaterThanOrEqualTo(1);
        assertThat(first.get("answer").asInt()).isPositive();
    }

    @Test
    @DisplayName("응답의 코드를 다시 넣으면 같은 학습지가 나온다")
    void sameCodeSameSheet() throws Exception {
        Signup owner = signupOwner("w3@example.com", "학원");
        Map<String, Object> req = Map.of(
                "grade", 4, "categories", List.of("MERGE", "COMPARE", "GROUP"), "count", 8, "carry", true);

        JsonNode first = bodyOf(call(post("/api/worksheets"), owner.token(), req));
        String code = first.get("code").asText();

        Map<String, Object> again = new java.util.HashMap<>(req);
        again.put("code", code);
        JsonNode second = bodyOf(call(post("/api/worksheets"), owner.token(), again));

        assertThat(second.get("code").asText()).isEqualTo(code);
        assertThat(second.get("problems").toString()).isEqualTo(first.get("problems").toString());
    }

    @Test
    @DisplayName("이상한 코드를 넣으면 막지 않고 새로 뽑아 준다")
    void garbageCodeFallsBackToNew() throws Exception {
        Signup owner = signupOwner("w4@example.com", "학원");
        MvcResult r = call(post("/api/worksheets"), owner.token(), Map.of(
                "grade", 3, "categories", List.of("MERGE"), "count", 3, "code", "없는코드"));

        assertThat(r.getResponse().getStatus()).isEqualTo(200);
        assertThat(bodyOf(r).get("problems")).hasSize(3);
    }

    @Test
    @DisplayName("학년에 맞지 않는 유형만 고르면 안내한다")
    void impossibleCombination() throws Exception {
        Signup owner = signupOwner("w5@example.com", "학원");
        MvcResult r = call(post("/api/worksheets"), owner.token(), Map.of(
                "grade", 1, "categories", List.of("SHARE"), "count", 5));

        assertThat(r.getResponse().getStatus()).isEqualTo(400);
        assertThat(bodyOf(r).get("code").asText()).isEqualTo("NO_TEMPLATE_FOR_GRADE");
    }

    @Test
    @DisplayName("학생 토큰으로는 학습지를 뽑을 수 없다 — 정답이 들어 있기 때문")
    void studentCannotGenerate() throws Exception {
        Signup owner = signupOwner("w6@example.com", "학원");
        call(post("/api/students"), owner.token(), Map.of("name", "김민우", "grade", 3, "pin", "1234"));
        String studentToken = bodyOf(call(post("/api/auth/student/login"), null,
                Map.of("academyCode", owner.academyCode(), "name", "김민우", "pin", "1234")))
                .get("token").asText();

        MvcResult r = call(post("/api/worksheets"), studentToken, Map.of(
                "grade", 3, "categories", List.of("MERGE"), "count", 3));
        assertThat(r.getResponse().getStatus()).isEqualTo(403);
    }
}
