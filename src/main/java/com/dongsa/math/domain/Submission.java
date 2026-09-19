package com.dongsa.math.domain;

import com.dongsa.math.problem.ProblemCategory;
import com.dongsa.math.problem.SolveStep;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 아이가 한 단계에 낸 답 하나.
 *
 * 한 단계는 한 번만 받는다. 같은 단계로 또 들어오면 처음 결과를 그대로 돌려준다 —
 * 폰은 잘 끊기고, 끊겼다고 다시 눌렀을 때 점수가 바뀌면 안 되기 때문이다.
 */
@Entity
@Getter
@Table(name = "submission",
        uniqueConstraints = @UniqueConstraint(name = "uk_submission_step",
                columnNames = {"student_assignment_id", "problem_no", "step"}),
        indexes = @Index(name = "ix_submission_sa", columnList = "student_assignment_id"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Submission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_assignment_id", nullable = false)
    private StudentAssignment studentAssignment;

    @Column(name = "problem_no", nullable = false)
    private int problemNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private SolveStep step;

    /** 아이가 실제로 고른 값. 무엇을 골라서 틀렸는지 봐야 가르칠 수 있다. */
    @Column(name = "submitted_value", nullable = false, length = 120)
    private String submittedValue;

    @Column(nullable = false)
    private boolean correct;

    /** 리포트에서 유형별로 묶기 위해 함께 저장한다. 매번 문제를 다시 만들지 않으려는 목적이다. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProblemCategory category;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;

    public Submission(StudentAssignment studentAssignment, int problemNo, SolveStep step,
                      String submittedValue, boolean correct, ProblemCategory category, Instant submittedAt) {
        this.studentAssignment = studentAssignment;
        this.problemNo = problemNo;
        this.step = step;
        this.submittedValue = submittedValue;
        this.correct = correct;
        this.category = category;
        this.submittedAt = submittedAt;
    }
}
