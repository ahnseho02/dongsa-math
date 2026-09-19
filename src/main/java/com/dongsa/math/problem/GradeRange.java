package com.dongsa.math.problem;

/** 학년별로 다루는 수의 크기. 교과 진도에 맞춘다. */
public record GradeRange(int grade, int min, int max, String label) {

    public static GradeRange of(int grade) {
        return switch (grade) {
            case 1 -> new GradeRange(1, 2, 9, "한 자리 수");
            case 2 -> new GradeRange(2, 10, 99, "두 자리 수");
            case 3 -> new GradeRange(3, 100, 999, "세 자리 수");
            case 4 -> new GradeRange(4, 1000, 9999, "네 자리 수");
            case 5 -> new GradeRange(5, 1000, 9999, "네 자리 수 · 곱셈과 나눗셈");
            case 6 -> new GradeRange(6, 10000, 99999, "다섯 자리 수");
            default -> throw new IllegalArgumentException("초등 1~6학년만 지원합니다: " + grade);
        };
    }
}
