package com.dongsa.math.problem;

/**
 * 문장이 어떤 상황인지에 따른 분류.
 * 아이가 배우는 것은 "이 상황에는 이 연산"이라는 연결이므로, 분류가 곧 학습 단위다.
 * 나중에 취약점 리포트도 이 단위로 뽑는다.
 */
public enum ProblemCategory {

    MERGE("합치기", 1),
    INCREASE("늘어나기", 1),
    DECREASE("줄어들기", 1),
    COMPARE("비교하기", 2),
    MULTI("여러 수", 2),
    GROUP("묶어 세기", 2),
    SHARE("똑같이 나누기", 3);

    private final String label;
    private final int minGrade;

    ProblemCategory(String label, int minGrade) {
        this.label = label;
        this.minGrade = minGrade;
    }

    public String label() { return label; }

    public int minGrade() { return minGrade; }
}
