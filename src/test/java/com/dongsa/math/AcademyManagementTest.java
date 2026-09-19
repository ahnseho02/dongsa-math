package com.dongsa.math;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@DisplayName("고모(원장) 계정이 학원을 관리한다")
class AcademyManagementTest extends ApiTestSupport {

    @Test
    @DisplayName("반을 만들고 학생을 반에 넣는다")
    void classroomAndStudents() throws Exception {
        Signup owner = signupOwner("cls@example.com", "동사수학학원");

        MvcResult cls = call(post("/api/classrooms"), owner.token(), Map.of("name", "월수 3학년반", "grade", 3));
        long classroomId = bodyOf(cls).get("id").asLong();

        call(post("/api/students"), owner.token(),
                Map.of("name", "김민우", "grade", 3, "pin", "1234", "classroomId", classroomId));
        call(post("/api/students"), owner.token(),
                Map.of("name", "박서윤", "grade", 3, "pin", "5678", "classroomId", classroomId));

        MvcResult inClass = call(get("/api/students?classroomId=" + classroomId), owner.token(), null);
        assertThat(bodyOf(inClass).size()).isEqualTo(2);

        MvcResult list = call(get("/api/classrooms"), owner.token(), null);
        assertThat(bodyOf(list).get(0).get("studentCount").asLong()).isEqualTo(2);
    }

    @Test
    @DisplayName("같은 이름의 반은 두 번 만들 수 없다")
    void duplicateClassroomName() throws Exception {
        Signup owner = signupOwner("dupcls@example.com", "학원");
        call(post("/api/classrooms"), owner.token(), Map.of("name", "화목반", "grade", 4));

        MvcResult r = call(post("/api/classrooms"), owner.token(), Map.of("name", "화목반", "grade", 5));
        assertThat(r.getResponse().getStatus()).isEqualTo(409);
    }

    @Test
    @DisplayName("반을 지워도 학생은 남는다")
    void deletingClassroomKeepsStudents() throws Exception {
        Signup owner = signupOwner("delcls@example.com", "학원");
        long classroomId = bodyOf(call(post("/api/classrooms"), owner.token(),
                Map.of("name", "금요반", "grade", 3))).get("id").asLong();
        call(post("/api/students"), owner.token(),
                Map.of("name", "김민우", "grade", 3, "pin", "1234", "classroomId", classroomId));

        call(delete("/api/classrooms/" + classroomId), owner.token(), null);

        JsonNode all = bodyOf(call(get("/api/students"), owner.token(), null));
        assertThat(all.size()).isEqualTo(1);
        assertThat(all.get(0).get("classroomId").isNull()).isTrue();
    }

    @Test
    @DisplayName("PIN 을 다시 발급하면 예전 PIN 으로는 로그인되지 않는다")
    void resetPin() throws Exception {
        Signup owner = signupOwner("pin@example.com", "학원");
        long id = bodyOf(call(post("/api/students"), owner.token(),
                Map.of("name", "김민우", "grade", 3, "pin", "1111"))).get("student").get("id").asLong();

        MvcResult reset = call(patch("/api/students/" + id + "/pin"), owner.token(), Map.of("pin", "2222"));
        assertThat(bodyOf(reset).get("pin").asText()).isEqualTo("2222");

        MvcResult old = call(post("/api/auth/student/login"), null,
                Map.of("academyCode", owner.academyCode(), "name", "김민우", "pin", "1111"));
        assertThat(old.getResponse().getStatus()).isEqualTo(401);

        MvcResult fresh = call(post("/api/auth/student/login"), null,
                Map.of("academyCode", owner.academyCode(), "name", "김민우", "pin", "2222"));
        assertThat(fresh.getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("원장은 강사 계정을 만들 수 있고, 강사는 만들 수 없다")
    void onlyOwnerCreatesTeachers() throws Exception {
        Signup owner = signupOwner("owner@example.com", "학원");

        MvcResult created = call(post("/api/academy/teachers"), owner.token(),
                Map.of("email", "teacher@example.com", "password", "password123", "name", "강사"));
        assertThat(created.getResponse().getStatus()).isEqualTo(201);
        assertThat(bodyOf(created).get("role").asText()).isEqualTo("TEACHER");

        String teacherToken = bodyOf(call(post("/api/auth/login"), null,
                Map.of("email", "teacher@example.com", "password", "password123"))).get("token").asText();

        MvcResult denied = call(post("/api/academy/teachers"), teacherToken,
                Map.of("email", "another@example.com", "password", "password123", "name", "또다른강사"));
        assertThat(denied.getResponse().getStatus()).isEqualTo(403);
        assertThat(bodyOf(denied).get("code").asText()).isEqualTo("OWNER_ONLY");
    }

    @Test
    @DisplayName("강사도 학생과 반은 관리할 수 있다")
    void teacherCanManageStudents() throws Exception {
        Signup owner = signupOwner("o2@example.com", "학원");
        call(post("/api/academy/teachers"), owner.token(),
                Map.of("email", "t2@example.com", "password", "password123", "name", "강사"));
        String teacherToken = bodyOf(call(post("/api/auth/login"), null,
                Map.of("email", "t2@example.com", "password", "password123"))).get("token").asText();

        MvcResult r = call(post("/api/students"), teacherToken, Map.of("name", "이시우", "grade", 4));
        assertThat(r.getResponse().getStatus()).isEqualTo(201);
    }

    @Test
    @DisplayName("중지된 강사는 로그인되지 않는다")
    void disabledTeacherCannotLogin() throws Exception {
        Signup owner = signupOwner("o3@example.com", "학원");
        long teacherId = bodyOf(call(post("/api/academy/teachers"), owner.token(),
                Map.of("email", "t3@example.com", "password", "password123", "name", "강사"))).get("id").asLong();

        call(patch("/api/academy/teachers/" + teacherId + "/active"), owner.token(), Map.of("active", false));

        MvcResult r = call(post("/api/auth/login"), null,
                Map.of("email", "t3@example.com", "password", "password123"));
        assertThat(r.getResponse().getStatus()).isEqualTo(403);
        assertThat(bodyOf(r).get("code").asText()).isEqualTo("ACCOUNT_DISABLED");
    }

    @Test
    @DisplayName("원장이 자기 계정을 중지시킬 수는 없다")
    void ownerCannotDisableSelf() throws Exception {
        Signup owner = signupOwner("self@example.com", "학원");
        MvcResult r = call(patch("/api/academy/teachers/" + owner.teacherId() + "/active"),
                owner.token(), Map.of("active", false));
        assertThat(r.getResponse().getStatus()).isEqualTo(400);
    }
}
