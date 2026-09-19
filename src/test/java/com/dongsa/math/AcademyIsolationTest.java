package com.dongsa.math;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/**
 * 학원끼리 데이터가 새지 않는지 확인한다.
 * 토큰에 학원 번호가 들어 있고 모든 조회가 그 번호로 걸러지므로,
 * 다른 학원의 id 를 알아도 "없음"으로 나와야 한다.
 */
@DisplayName("학원 경계")
class AcademyIsolationTest extends ApiTestSupport {

    @Test
    @DisplayName("다른 학원 학생은 목록에도 안 보이고 직접 불러도 404")
    void cannotSeeOtherAcademyStudents() throws Exception {
        Signup a = signupOwner("a@example.com", "가학원");
        Signup b = signupOwner("b@example.com", "나학원");

        long studentOfA = bodyOf(call(post("/api/students"), a.token(),
                Map.of("name", "가학원학생", "grade", 3, "pin", "1234"))).get("student").get("id").asLong();

        assertThat(bodyOf(call(get("/api/students"), b.token(), null)).size()).isZero();

        MvcResult r = call(patch("/api/students/" + studentOfA), b.token(), Map.of("grade", 6));
        assertThat(r.getResponse().getStatus()).isEqualTo(404);
    }

    @Test
    @DisplayName("다른 학원 반에 학생을 넣을 수 없다")
    void cannotAssignToOtherAcademyClassroom() throws Exception {
        Signup a = signupOwner("ca@example.com", "가학원");
        Signup b = signupOwner("cb@example.com", "나학원");

        long classroomOfA = bodyOf(call(post("/api/classrooms"), a.token(),
                Map.of("name", "가반", "grade", 3))).get("id").asLong();

        MvcResult r = call(post("/api/students"), b.token(),
                Map.of("name", "나학원학생", "grade", 3, "pin", "1234", "classroomId", classroomOfA));

        assertThat(r.getResponse().getStatus()).isEqualTo(404);
        assertThat(bodyOf(r).get("code").asText()).isEqualTo("CLASSROOM_NOT_FOUND");
    }

    @Test
    @DisplayName("다른 학원 코드로는 우리 학생이 로그인되지 않는다")
    void studentCannotLoginWithOtherAcademyCode() throws Exception {
        Signup a = signupOwner("la@example.com", "가학원");
        Signup b = signupOwner("lb@example.com", "나학원");
        call(post("/api/students"), a.token(), Map.of("name", "김민우", "grade", 3, "pin", "1234"));

        MvcResult r = call(post("/api/auth/student/login"), null,
                Map.of("academyCode", b.academyCode(), "name", "김민우", "pin", "1234"));

        assertThat(r.getResponse().getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("다른 학원 강사를 중지시킬 수 없다")
    void cannotDisableOtherAcademyTeacher() throws Exception {
        Signup a = signupOwner("ta@example.com", "가학원");
        Signup b = signupOwner("tb@example.com", "나학원");

        MvcResult r = call(patch("/api/academy/teachers/" + a.teacherId() + "/active"),
                b.token(), Map.of("active", false));

        assertThat(r.getResponse().getStatus()).isEqualTo(404);
    }
}
