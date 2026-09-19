package com.dongsa.math.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.Instant;

/** 연산 게임 한 판. 문제는 저장하지 않고 씨앗만 둔다. */
@Entity
@Getter
@Table(name = "game_round", indexes = {
        @Index(name = "ix_game_ranking", columnList = "academy_id,solved,elapsed_ms"),
        @Index(name = "ix_game_student", columnList = "student_id,id")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GameRound {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "academy_id", nullable = false)
    private Academy academy;

    @Column(nullable = false)
    private int grade;

    @Column(nullable = false)
    private long seed;

    @Column(name = "problem_count", nullable = false)
    private int problemCount;

    @Column(name = "limit_seconds", nullable = false)
    private int limitSeconds;

    @Column(nullable = false)
    private int attempted;

    @Column(nullable = false)
    private int solved;

    @Column(name = "elapsed_ms")
    private Integer elapsedMs;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    public GameRound(Student student, int grade, long seed, int problemCount, int limitSeconds, Instant startedAt) {
        this.student = student;
        this.academy = student.getAcademy();
        this.grade = grade;
        this.seed = seed;
        this.problemCount = problemCount;
        this.limitSeconds = limitSeconds;
        this.attempted = 0;
        this.solved = 0;
        this.startedAt = startedAt;
    }

    public boolean isFinished() {
        return finishedAt != null;
    }

    /** 제한 시간에 여유를 조금 준다 — 마지막 답이 네트워크를 타고 오는 시간이 있다. */
    public boolean isTooLate(Instant now) {
        return Duration.between(startedAt, now).getSeconds() > limitSeconds + 10L;
    }

    public void finish(int attempted, int solved, Instant now) {
        this.attempted = attempted;
        this.solved = solved;
        this.finishedAt = now;
        this.elapsedMs = (int) Math.min(Integer.MAX_VALUE, Duration.between(startedAt, now).toMillis());
    }
}
