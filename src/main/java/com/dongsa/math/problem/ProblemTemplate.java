package com.dongsa.math.problem;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 문제 한 종류. 문장은 여기 있고, 숫자는 낼 때마다 새로 만든다.
 *
 * pattern 표기:
 *   {name} {name2}  — 아이 이름이 들어간다
 *   {a} {b} {c}     — 숫자가 들어간다
 *   {은/는}          — 앞 글자를 보고 조사를 고른다
 *   «모두»           — 아이가 눌러서 고를 수 있는 단어. 그중 cues 에 든 것이 정답
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

    /** 사람이 알아보는 짧은 이름. 시드를 고정해도 템플릿을 알아볼 수 있게 둔다. */
    @Column(nullable = false, length = 20)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProblemCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Operation operation;

    @Enumerated(EnumType.STRING)
    @Column(name = "number_pattern", nullable = false, length = 20)
    private NumberPattern numberPattern;

    @Column(nullable = false, length = 600)
    private String pattern;

    /** 답의 단위. "개", "쪽", "원" */
    @Column(nullable = false, length = 10)
    private String unit;

    /** 이 문장이 자연스러운 학년 범위. "색 테이프 41273cm" 같은 문장을 막는다. */
    @Column(name = "min_grade", nullable = false)
    private int minGrade;

    @Column(name = "max_grade", nullable = false)
    private int maxGrade;

    /** 연산을 알려 주는 단서 단어. 여러 개면 아무거나 맞으면 정답. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "problem_template_cue",
            joinColumns = @JoinColumn(name = "template_id"),
            foreignKey = @ForeignKey(name = "fk_cue_template"))
    @Column(name = "word", nullable = false, length = 40)
    private List<String> cues = new ArrayList<>();

    /** 두 번째 숫자에 곱할 값. "한 권에 900원" 처럼 100 단위로 떨어뜨릴 때만 쓴다. */
    @Column(name = "second_scale", nullable = false)
    private int secondScale = 1;

    /** 이름이 둘 필요한 문장인지. "민우보다 서윤이가 더 많이" */
    @Column(name = "needs_two_names", nullable = false)
    private boolean needsTwoNames;

    public ProblemTemplate(String code, ProblemCategory category, Operation operation, NumberPattern numberPattern,
                           String pattern, String unit, int minGrade, int maxGrade,
                           List<String> cues, int secondScale, boolean needsTwoNames) {
        this.code = code;
        this.category = category;
        this.operation = operation;
        this.numberPattern = numberPattern;
        this.pattern = pattern;
        this.unit = unit;
        this.minGrade = minGrade;
        this.maxGrade = maxGrade;
        this.cues = new ArrayList<>(cues);
        this.secondScale = secondScale;
        this.needsTwoNames = needsTwoNames;
    }

    /** JSON 의 내용이 바뀌면 그대로 따라간다. JSON 이 문장의 원본이다. */
    public boolean syncFrom(ProblemTemplate other) {
        boolean changed = !pattern.equals(other.pattern)
                || !unit.equals(other.unit)
                || !List.copyOf(cues).equals(List.copyOf(other.cues))
                || category != other.category
                || operation != other.operation
                || numberPattern != other.numberPattern
                || minGrade != other.minGrade
                || maxGrade != other.maxGrade
                || secondScale != other.secondScale
                || needsTwoNames != other.needsTwoNames;
        if (!changed) {
            return false;
        }
        this.category = other.category;
        this.operation = other.operation;
        this.numberPattern = other.numberPattern;
        this.pattern = other.pattern;
        this.unit = other.unit;
        this.minGrade = other.minGrade;
        this.maxGrade = other.maxGrade;
        this.cues.clear();
        this.cues.addAll(other.cues);
        this.secondScale = other.secondScale;
        this.needsTwoNames = other.needsTwoNames;
        return true;
    }

    public boolean fitsGrade(int grade) {
        return grade >= minGrade && grade <= maxGrade;
    }
}
