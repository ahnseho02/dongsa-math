package com.dongsa.math;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * 학생 PIN 은 네 자리라 1만 가지뿐이다.
 * 학원 코드와 이름만 알면 전부 넣어 보는 데 오래 걸리지 않는다 — 그래서 횟수를 막는다.
 */
@DisplayName("로그인 시도 제한")
@TestPropertySource(properties = {
        "app.login.max-failures=3",
        "app.login.max-failures-per-ip=100",
        "app.login.window=PT10M"
})
class LoginThrottleTest extends ApiTestSupport {

    @Autowired com.dongsa.math.security.LoginAttemptLimiter limiter;

    @Test
    @DisplayName("PIN 을 계속 틀리면 막힌다")
    void studentPinBruteForceIsBlocked() throws Exception {
        Signup owner = signupOwner("brute@example.com", "학원");
        call(post("/api/students"), owner.token(), Map.of("name", "김민우", "grade", 3, "pin", "1234"));

        for (int i = 0; i < 3; i++) {
            MvcResult r = studentLogin(owner.academyCode(), "김민우", "000" + i);
            assertThat(r.getResponse().getStatus()).as("%d번째 시도", i + 1).isEqualTo(401);
        }

        MvcResult blocked = studentLogin(owner.academyCode(), "김민우", "0009");
        assertThat(blocked.getResponse().getStatus()).isEqualTo(429);
        assertThat(bodyOf(blocked).get("code").asText()).isEqualTo("TOO_MANY_ATTEMPTS");

        // 막힌 뒤에는 진짜 PIN 을 넣어도 통과하지 못한다 — 대조 자체를 하지 않는다
        assertThat(studentLogin(owner.academyCode(), "김민우", "1234").getResponse().getStatus())
                .isEqualTo(429);
    }

    @Test
    @DisplayName("제대로 들어오면 실패 기록이 지워진다")
    void successResetsTheCounter() throws Exception {
        Signup owner = signupOwner("reset@example.com", "학원");
        call(post("/api/students"), owner.token(), Map.of("name", "박서윤", "grade", 3, "pin", "4321"));

        studentLogin(owner.academyCode(), "박서윤", "0000");
        studentLogin(owner.academyCode(), "박서윤", "1111");
        assertThat(studentLogin(owner.academyCode(), "박서윤", "4321").getResponse().getStatus())
                .as("아직 한도 안").isEqualTo(200);

        // 앞서 두 번 틀린 기록이 지워졌으므로 다시 세 번까지 견딘다
        for (int i = 0; i < 3; i++) {
            assertThat(studentLogin(owner.academyCode(), "박서윤", "000" + i).getResponse().getStatus())
                    .isEqualTo(401);
        }
        assertThat(studentLogin(owner.academyCode(), "박서윤", "9999").getResponse().getStatus())
                .isEqualTo(429);
    }

    @Test
    @DisplayName("한 아이가 막혀도 다른 아이는 멀쩡하다")
    void blockingIsPerAccount() throws Exception {
        Signup owner = signupOwner("peracc@example.com", "학원");
        call(post("/api/students"), owner.token(), Map.of("name", "가가가", "grade", 3, "pin", "1111"));
        call(post("/api/students"), owner.token(), Map.of("name", "나나나", "grade", 3, "pin", "2222"));

        for (int i = 0; i < 4; i++) {
            studentLogin(owner.academyCode(), "가가가", "000" + i);
        }
        assertThat(studentLogin(owner.academyCode(), "가가가", "1111").getResponse().getStatus()).isEqualTo(429);
        assertThat(studentLogin(owner.academyCode(), "나나나", "2222").getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("선생님 로그인도 같은 제한을 받는다")
    void teacherLoginIsThrottled() throws Exception {
        signupOwner("throttled@example.com", "학원");

        for (int i = 0; i < 3; i++) {
            MvcResult r = call(post("/api/auth/login"), null,
                    Map.of("email", "throttled@example.com", "password", "wrong-password-" + i));
            assertThat(r.getResponse().getStatus()).isEqualTo(401);
        }
        MvcResult blocked = call(post("/api/auth/login"), null,
                Map.of("email", "throttled@example.com", "password", "password123"));
        assertThat(blocked.getResponse().getStatus()).isEqualTo(429);
    }

    @Test
    @DisplayName("없는 계정을 훑는 것도 똑같이 센다 — 가입 여부가 새어 나가지 않는다")
    void unknownAccountsAreCountedToo() throws Exception {
        for (int i = 0; i < 3; i++) {
            assertThat(call(post("/api/auth/login"), null,
                    Map.of("email", "nobody@example.com", "password", "x" + i))
                    .getResponse().getStatus()).isEqualTo(401);
        }
        assertThat(call(post("/api/auth/login"), null,
                Map.of("email", "nobody@example.com", "password", "y"))
                .getResponse().getStatus()).isEqualTo(429);
    }

    private MvcResult studentLogin(String code, String name, String pin) throws Exception {
        return call(post("/api/auth/student/login"), null,
                Map.of("academyCode", code, "name", name, "pin", pin));
    }
}
