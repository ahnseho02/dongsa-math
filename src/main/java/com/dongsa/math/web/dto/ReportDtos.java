package com.dongsa.math.web.dto;

import java.util.List;

import java.time.Instant;

public final class ReportDtos {

    private ReportDtos() {}

    public record Rate(String key, String label, long correct, long total, int percent) {

        public static Rate of(String key, String label, long correct, long total) {
            return new Rate(key, label, correct, total, total == 0 ? 0 : Math.round(correct * 100f / total));
        }
    }

    /**
     * 과제 리포트. 이 표가 이 서비스가 파는 것이다.
     * 같은 60점이어도 '단서'가 낮은 아이와 '답'이 낮은 아이는 처방이 다르다.
     */
    public record AssignmentReport(
            Long assignmentId,
            String title,
            long totalStudents,
            long completedStudents,
            List<Rate> bySteps,
            List<Rate> byCategories,
            List<StudentRow> students,
            String weakestStep,
            String advice) {}

    public record StudentRow(
            Long studentId,
            String studentName,
            String status,
            int percent,
            List<Rate> bySteps) {}

    /** 한 아이가 그동안 푼 것 전부. 학기 단위로 "이 아이는 뭘 못하나" 를 본다. */
    public record StudentReport(
            Long studentId,
            String studentName,
            int grade,
            boolean active,
            long assignmentCount,
            long completedCount,
            long correctSteps,
            long answeredSteps,
            int percent,
            List<Rate> bySteps,
            List<Rate> byCategories,
            List<AssignmentScore> assignments,
            String weakestStep,
            String advice) {}

    public record AssignmentScore(
            Long assignmentId,
            String title,
            String status,
            long correct,
            int total,
            int percent,
            Instant createdAt) {}

    /** 학원 학생 전체를 한 줄씩. 누가 어느 단계에서 막히는지 목록으로 본다. */
    public record AcademyStudentRow(
            Long studentId,
            String studentName,
            int grade,
            boolean active,
            long assignmentCount,
            long answeredSteps,
            int percent,
            List<Rate> bySteps) {}
}
