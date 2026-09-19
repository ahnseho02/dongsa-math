package com.dongsa.math.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/** 한 아이에게 배정된 과제. 아이마다 문제가 다를 수 있으므로 씨앗을 따로 들고 있다. */
@Entity
@Getter
@Table(name = "student_assignment",
        uniqueConstraints = @UniqueConstraint(name = "uk_student_assignment",
                columnNames = {"assignment_id", "student_id"}),
        indexes = @Index(name = "ix_student_assignment_student", columnList = "student_id,id"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudentAssignment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assignment_id", nullable = false)
    private Assignment assignment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(nullable = false)
    private long seed;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    public StudentAssignment(Assignment assignment, Student student) {
        this.assignment = assignment;
        this.student = student;
        this.seed = assignment.seedFor(student.getId());
    }

    public void markStarted(Instant when) {
        if (startedAt == null) {
            startedAt = when;
        }
    }

    public void markCompleted(Instant when) {
        markStarted(when);
        if (completedAt == null) {
            completedAt = when;
        }
    }

    public String status() {
        if (completedAt != null) {
            return "DONE";
        }
        return startedAt != null ? "IN_PROGRESS" : "NOT_STARTED";
    }
}
