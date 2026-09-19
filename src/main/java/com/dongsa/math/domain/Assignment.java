package com.dongsa.math.domain;

import com.dongsa.math.problem.ProblemCategory;
import com.dongsa.math.problem.ReadableCode;
import com.dongsa.math.problem.WorksheetSpec;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 과제 한 건. 문제를 저장하지 않고 **만드는 조건만** 저장한다.
 * 조건과 시드가 같으면 언제 다시 만들어도 같은 문제가 나오기 때문이다.
 */
@Entity
@Getter
@Table(name = "assignment", indexes = {
        @Index(name = "ix_assignment_academy", columnList = "academy_id,id"),
        @Index(name = "ix_assignment_classroom", columnList = "classroom_id")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Assignment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "academy_id", nullable = false)
    private Academy academy;

    /** 반 전체에 낸 과제면 반이 들어가고, 몇 명만 골라 냈으면 비어 있다. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classroom_id")
    private Classroom classroom;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private Teacher createdBy;

    @Column(nullable = false, length = 80)
    private String title;

    @Column(nullable = false)
    private int grade;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "assignment_category",
            joinColumns = @JoinColumn(name = "assignment_id"),
            foreignKey = @ForeignKey(name = "fk_assignment_category"))
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 20)
    private List<ProblemCategory> categories = new ArrayList<>();

    @Column(name = "problem_count", nullable = false)
    private int problemCount;

    @Column(nullable = false)
    private boolean carry;

    /** 문제를 만들어 낸 씨앗. 이 값이 있으면 문제를 저장하지 않아도 된다. */
    @Column(nullable = false)
    private long seed;

    /** true 면 반 전체가 같은 문제, false 면 아이마다 다른 문제(베끼기 방지). */
    @Column(name = "same_for_everyone", nullable = false)
    private boolean sameForEveryone;

    @Column(name = "due_at")
    private Instant dueAt;

    /** 마감하면 더 이상 답을 받지 않는다. 기록은 그대로 둔다. */
    @Column(name = "closed_at")
    private Instant closedAt;

    public Assignment(Academy academy, Classroom classroom, Teacher createdBy, String title,
                      WorksheetSpec spec, boolean sameForEveryone, Instant dueAt) {
        this.academy = academy;
        this.classroom = classroom;
        this.createdBy = createdBy;
        this.title = title;
        this.grade = spec.grade();
        this.categories = new ArrayList<>(spec.categories());
        this.problemCount = spec.count();
        this.carry = spec.carry();
        this.seed = spec.seed();
        this.sameForEveryone = sameForEveryone;
        this.dueAt = dueAt;
    }

    public WorksheetSpec specWithSeed(long studentSeed) {
        return new WorksheetSpec(grade, categories, problemCount, carry, studentSeed);
    }

    /**
     * 아이마다 다른 문제를 낼 때 쓰는 씨앗.
     * 과제 씨앗과 학생 번호를 섞어서 만든다 — 저장해 두면 나중에 그대로 다시 만들 수 있다.
     */
    public long seedFor(Long studentId) {
        if (sameForEveryone) {
            return seed;
        }
        long mixed = seed * 1_000_003L + studentId * 31L;
        return Math.floorMod(mixed, ReadableCode.MAX_SEED);
    }

    public String code() {
        return ReadableCode.encode(seed);
    }

    public boolean isClosed() {
        return closedAt != null;
    }

    /** 마감 시각이 지났거나 선생님이 닫았으면 더 받지 않는다. */
    public boolean acceptsAnswers(Instant now) {
        if (isClosed()) {
            return false;
        }
        return dueAt == null || now.isBefore(dueAt);
    }

    public void close(Instant when) {
        this.closedAt = when;
    }

    public void reopen() {
        this.closedAt = null;
    }

    public int totalSteps() {
        return problemCount * com.dongsa.math.problem.SolveStep.COUNT;
    }
}
