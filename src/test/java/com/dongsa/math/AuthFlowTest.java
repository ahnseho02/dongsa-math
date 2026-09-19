package com.dongsa.math;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@DisplayName("회원가입부터 학생 로그인까지")
class AuthFlowTest extends ApiTestSupport {

    @Test
    @DisplayName("원장이 가입하면 학원과 학원 코드가 같이 생긴다")
    void signupCreatesAcademy() throws Exception {
        MvcResult r = call(post("/api/auth/signup"), null, Map.of(
                "email", "gomo@example.com", "password", "password123",
                "name", "고모", "academyName", "동사수학학원"));

        assertThat(r.getResponse().getStatus()).isEqualTo(201);
        JsonNode body = bodyOf(r);
        assertThat(body.get("token").asText()).isNotBlank();
        assertThat(body.get("user").get("role").asText()).isEqualTo("OWNER");
        assertThat(body.get("user").get("academyCode").asText())
                .hasSize(6)
                .matches("[ABCDEFGHJKLMNPQRSTUVWXYZ23456789]{6}");   // 헷갈리는 글자는 안 쓴다
    }

    @Test
    @DisplayName("같은 이메일로 두 번 가입할 수 없다")
    void duplicateEmailRejected() throws Exception {
        signupOwner("dup@example.com", "학원1");
        MvcResult r = call(post("/api/auth/signup"), null, Map.of(
                "email", "dup@example.com", "password", "password123",
                "name", "다른사람", "academyName", "학원2"));

        assertThat(r.getResponse().getStatus()).isEqualTo(409);
        assertThat(bodyOf(r).get("code").asText()).isEqualTo("EMAIL_TAKEN");
    }

    @Test
    @DisplayName("이메일 대소문자가 달라도 같은 계정으로 로그인된다")
    void emailIsCaseInsensitive() throws Exception {
        signupOwner("Case@Example.com", "학원");
        MvcResult r = call(post("/api/auth/login"), null,
                Map.of("email", "CASE@EXAMPLE.COM", "password", "password123"));

        assertThat(r.getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("비밀번호가 틀리면 401, 이유는 알려 주지 않는다")
    void wrongPassword() throws Exception {
        signupOwner("pw@example.com", "학원");
        MvcResult r = call(post("/api/auth/login"), null,
                Map.of("email", "pw@example.com", "password", "wrong-password"));

        assertThat(r.getResponse().getStatus()).isEqualTo(401);
        assertThat(bodyOf(r).get("code").asText()).isEqualTo("BAD_CREDENTIALS");
    }

    @Test
    @DisplayName("짧은 비밀번호는 어느 칸이 왜 틀렸는지 알려 준다")
    void validationTellsWhichField() throws Exception {
        MvcResult r = call(post("/api/auth/signup"), null, Map.of(
                "email", "short@example.com", "password", "123",
                "name", "고모", "academyName", "학원"));

        assertThat(r.getResponse().getStatus()).isEqualTo(400);
        assertThat(bodyOf(r).get("fields").has("password")).isTrue();
    }

    @Test
    @DisplayName("학생은 학원 코드 + 이름 + PIN 으로 로그인한다")
    void studentLogin() throws Exception {
        Signup owner = signupOwner("s@example.com", "동사수학학원");

        MvcResult created = call(post("/api/students"), owner.token(),
                Map.of("name", "김민우", "grade", 3, "pin", "1234"));
        assertThat(created.getResponse().getStatus()).isEqualTo(201);
        assertThat(bodyOf(created).get("pin").asText()).isEqualTo("1234");

        MvcResult login = call(post("/api/auth/student/login"), null,
                Map.of("academyCode", owner.academyCode(), "name", "김민우", "pin", "1234"));

        assertThat(login.getResponse().getStatus()).isEqualTo(200);
        JsonNode user = bodyOf(login).get("user");
        assertThat(user.get("kind").asText()).isEqualTo("STUDENT");
        assertThat(user.get("grade").asInt()).isEqualTo(3);
    }

    @Test
    @DisplayName("PIN 을 비워 두면 서버가 네 자리로 만들어 준다")
    void pinIsGeneratedWhenBlank() throws Exception {
        Signup owner = signupOwner("auto@example.com", "학원");
        MvcResult r = call(post("/api/students"), owner.token(),
                Map.of("name", "박서윤", "grade", 2));

        assertThat(bodyOf(r).get("pin").asText()).matches("\\d{4}");
    }

    @Test
    @DisplayName("PIN 이 틀리면 학생 로그인이 막힌다")
    void studentWrongPin() throws Exception {
        Signup owner = signupOwner("wp@example.com", "학원");
        call(post("/api/students"), owner.token(), Map.of("name", "김민우", "grade", 3, "pin", "1234"));

        MvcResult r = call(post("/api/auth/student/login"), null,
                Map.of("academyCode", owner.academyCode(), "name", "김민우", "pin", "9999"));

        assertThat(r.getResponse().getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("동명이인은 PIN 으로 갈린다")
    void sameNameDifferentPin() throws Exception {
        Signup owner = signupOwner("twin@example.com", "학원");
        call(post("/api/students"), owner.token(), Map.of("name", "이준", "grade", 3, "pin", "1111"));
        call(post("/api/students"), owner.token(), Map.of("name", "이준", "grade", 5, "pin", "2222"));

        MvcResult r = call(post("/api/auth/student/login"), null,
                Map.of("academyCode", owner.academyCode(), "name", "이준", "pin", "2222"));

        assertThat(bodyOf(r).get("user").get("grade").asInt()).isEqualTo(5);
    }

    @Test
    @DisplayName("이름도 PIN 도 같으면 누구인지 정할 수 없으므로 등록 단계에서 막는다")
    void sameNameSamePinRejected() throws Exception {
        Signup owner = signupOwner("clash@example.com", "학원");
        call(post("/api/students"), owner.token(), Map.of("name", "이준", "grade", 3, "pin", "1111"));

        MvcResult r = call(post("/api/students"), owner.token(),
                Map.of("name", "이준", "grade", 4, "pin", "1111"));

        assertThat(r.getResponse().getStatus()).isEqualTo(409);
        assertThat(bodyOf(r).get("code").asText()).isEqualTo("DUPLICATE_STUDENT_PIN");
    }

    @Test
    @DisplayName("중지된 학생은 로그인되지 않는다")
    void inactiveStudentCannotLogin() throws Exception {
        Signup owner = signupOwner("off@example.com", "학원");
        MvcResult created = call(post("/api/students"), owner.token(),
                Map.of("name", "홍길동", "grade", 3, "pin", "4321"));
        long id = bodyOf(created).get("student").get("id").asLong();

        call(patch("/api/students/" + id + "/active"), owner.token(), Map.of("active", false));

        MvcResult r = call(post("/api/auth/student/login"), null,
                Map.of("academyCode", owner.academyCode(), "name", "홍길동", "pin", "4321"));
        assertThat(r.getResponse().getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("토큰 없이 관리 API 를 부르면 401")
    void noTokenRejected() throws Exception {
        MvcResult r = call(get("/api/students"), null, null);
        assertThat(r.getResponse().getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("학생 토큰으로는 관리 API 를 쓸 수 없다")
    void studentCannotManage() throws Exception {
        Signup owner = signupOwner("sm@example.com", "학원");
        call(post("/api/students"), owner.token(), Map.of("name", "김민우", "grade", 3, "pin", "1234"));
        MvcResult login = call(post("/api/auth/student/login"), null,
                Map.of("academyCode", owner.academyCode(), "name", "김민우", "pin", "1234"));
        String studentToken = bodyOf(login).get("token").asText();

        MvcResult r = call(get("/api/students"), studentToken, null);
        assertThat(r.getResponse().getStatus()).isEqualTo(403);
    }
}
