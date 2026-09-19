package com.dongsa.math.web.dto;

import com.dongsa.math.problem.ProblemCategory;
import jakarta.validation.constraints.*;

import java.time.Instant;
import java.util.List;

public final class AssignmentDtos {

    private AssignmentDtos() {}

    /**
     * 과제 내주기.
     * 반을 고르면 그 반 전체, 학생을 고르면 그 아이들에게만 나간다. 둘 다 주면 합쳐진다.
     */
    public record CreateRequest(
            @NotBlank @Size(max = 80) String title,
            @NotNull @Min(1) @Max(6) Integer grade,
            @NotNull @Size(min = 1) List<ProblemCategory> categories,
            @Min(1) @Max(50) Integer count,
            Boolean carry,
            String code,
            Long classroomId,
            List<Long> studentIds,
            Boolean sameForEveryone,
            Instant dueAt) {

        public int countOrDefault() { return count == null ? 10 : count; }

        public boolean carryOrDefault() { return carry == null || carry; }

        /** 기본은 아이마다 다른 문제 — 옆자리를 보고 베끼지 못하게. */
        public boolean sameForEveryoneOrDefault() { return Boolean.TRUE.equals(sameForEveryone); }
    }

    public record AssignmentSummary(
            Long id,
            String title,
            int grade,
            int problemCount,
            String code,
            boolean sameForEveryone,
            Long classroomId,
            String classroomName,
            long studentCount,
            long completedCount,
            Instant dueAt,
            boolean closed,
            Instant createdAt) {}

    public record StudentProgress(
            Long studentAssignmentId,
            Long studentId,
            String studentName,
            String status,
            long correct,
            long submitted,
            int total,
            int percent) {}

    public record AssignmentDetail(AssignmentSummary assignment, List<StudentProgress> students) {}

    public record CloseRequest(boolean closed) {}
}
