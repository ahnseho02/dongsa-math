package com.dongsa.math;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@DisplayName("반을 지울 때")
class ClassroomDeletionTest extends ApiTestSupport {

    @Test
    @DisplayName("그 반에 낸 과제가 있어도 지워지고, 과제와 기록은 남는다")
    void deletingClassroomKeepsAssignments() throws Exception {
        Signup owner = signupOwner("delcls2@example.com", "학원");

        long classroomId = bodyOf(call(post("/api/classrooms"), owner.token(),
                Map.of("name", "월수반", "grade", 3))).get("id").asLong();
        call(post("/api/students"), owner.token(),
                Map.of("name", "김민우", "grade", 3, "pin", "1234", "classroomId", classroomId));

        long assignmentId = bodyOf(call(post("/api/assignments"), owner.token(), new HashMap<>(Map.of(
                "title", "반 숙제", "grade", 3, "categories", List.of("MERGE"), "count", 2,
                "classroomId", classroomId)))).get("assignment").get("id").asLong();

        assertThat(call(delete("/api/classrooms/" + classroomId), owner.token(), null)
                .getResponse().getStatus()).as("외래키에 걸려 실패하면 안 된다").isEqualTo(204);

        JsonNode detail = bodyOf(call(get("/api/assignments/" + assignmentId), owner.token(), null));
        assertThat(detail.get("assignment").get("classroomId").isNull()).isTrue();
        assertThat(detail.get("students")).hasSize(1);

        assertThat(bodyOf(call(get("/api/students"), owner.token(), null))
                .get(0).get("classroomId").isNull()).isTrue();
    }
}
