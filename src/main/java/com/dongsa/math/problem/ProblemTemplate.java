package com.dongsa.math.problem;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 문제 한 종류. 문장은 여기 있고, 숫자는 낼 때마다 새로 만든다.
 *
 * pattern 표기:
 *   {name} {name2}  — 아이 이름이 들어간다
 *   {a} {b} {c}     — 숫자가 들어간다 ({b%} 는 퍼센트로)
 *   {은/는}          — 앞 글자를 보고 조사를 고른다
 *   «모두»           — 아이가 눌러서 고를 수 있는 단어. 그중 cues 에 든 것이 정답
 *
 * operandSlots 는 식에 들어가는 슬롯을 순서대로 적은 것이다.
 *   "a,b"    보통
 *   "a,b,c"  세 수
 *   "a,b,a"  두 단계 문제에서 같은 수를 두 번 쓸 때 (고등어 420 − 135, 그리고 + 420)
 * 슬롯 중 여기 빠진 것은 문장에만 나오는 함정 숫자다.
 */
@Entity
@Getter
@Table(name = "problem_template",
        uniqueConstraints = @UniqueConstraint(name = "uk_template_code", columnNames = "code"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProblemTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProblemCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Operation operation;

    /** 두 단계 문제의 두 번째 연산. 한 단계면 비어 있다. */
    @Enumerated(EnumType.STRING)
    @Column(name = "second_operation", length = 20)
    private Operation secondOperation;

    @Enumerated(EnumType.STRING)
    @Column(name = "number_pattern", nullable = false, length = 20)
    private NumberPattern numberPattern;

    @Column(nullable = false, length = 600)
    private String pattern;

    @Column(nullable = false, length = 10)
    private String unit;

    @Column(name = "min_grade", nullable = false)
    private int minGrade;

    @Column(name = "max_grade", nullable = false)
    private int maxGrade;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "problem_template_cue",
            joinColumns = @JoinColumn(name = "template_id"),
            foreignKey = @ForeignKey(name = "fk_cue_template"))
    @Column(name = "word", nullable = false, length = 40)
    private List<String> cues = new ArrayList<>();

    @Column(name = "second_scale", nullable = false)
    private int secondScale = 1;

    @Column(name = "needs_two_names", nullable = false)
    private boolean needsTwoNames;

    @Column(name = "operand_slots", nullable = false, length = 20)
    private String operandSlots = "a,b";

    /** 기본 학습지에는 안 나오고 '도전' 을 켰을 때만 나온다. */
    @Column(nullable = false)
    private boolean hard;

    public ProblemTemplate(String code, ProblemCategory category, Operation operation, Operation secondOperation,
                           NumberPattern numberPattern, String pattern, String unit, int minGrade, int maxGrade,
                           List<String> cues, int secondScale, boolean needsTwoNames,
                           String operandSlots, boolean hard) {
        this.code = code;
        this.category = category;
        this.operation = operation;
        this.secondOperation = secondOperation;
        this.numberPattern = numberPattern;
        this.pattern = pattern;
        this.unit = unit;
        this.minGrade = minGrade;
        this.maxGrade = maxGrade;
        this.cues = new ArrayList<>(cues);
        this.secondScale = secondScale;
        this.needsTwoNames = needsTwoNames;
        this.operandSlots = operandSlots;
        this.hard = hard;
    }

    public List<Operation> operations() {
        return secondOperation == null ? List.of(operation) : List.of(operation, secondOperation);
    }

    /** "a,b,a" → [0, 1, 0] */
    public List<Integer> operandSlotIndexes() {
        return Arrays.stream(operandSlots.split(","))
                .map(s -> s.strip().charAt(0) - 'a')
                .toList();
    }

    /** JSON 의 내용이 바뀌면 그대로 따라간다. JSON 이 문장의 원본이다. */
    public boolean syncFrom(ProblemTemplate other) {
        boolean changed = !pattern.equals(other.pattern)
                || !unit.equals(other.unit)
                || !List.copyOf(cues).equals(List.copyOf(other.cues))
                || category != other.category
                || operation != other.operation
                || secondOperation != other.secondOperation
                || numberPattern != other.numberPattern
                || minGrade != other.minGrade
                || maxGrade != other.maxGrade
                || secondScale != other.secondScale
                || needsTwoNames != other.needsTwoNames
                || !operandSlots.equals(other.operandSlots)
                || hard != other.hard;
        if (!changed) {
            return false;
        }
        this.category = other.category;
        this.operation = other.operation;
        this.secondOperation = other.secondOperation;
        this.numberPattern = other.numberPattern;
        this.pattern = other.pattern;
        this.unit = other.unit;
        this.minGrade = other.minGrade;
        this.maxGrade = other.maxGrade;
        this.cues.clear();
        this.cues.addAll(other.cues);
        this.secondScale = other.secondScale;
        this.needsTwoNames = other.needsTwoNames;
        this.operandSlots = other.operandSlots;
        this.hard = other.hard;
        return true;
    }

    public boolean fitsGrade(int grade) {
        return grade >= minGrade && grade <= maxGrade;
    }
}
