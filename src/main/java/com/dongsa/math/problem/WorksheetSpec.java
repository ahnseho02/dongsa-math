package com.dongsa.math.problem;

import java.util.List;

/**
 * 학습지 한 장을 만드는 조건.
 * 이 다섯 가지가 같으면 언제 만들어도 똑같은 문제가 나온다 — 시드가 들어 있기 때문이다.
 */
public record WorksheetSpec(
        int grade,
        List<ProblemCategory> categories,
        int count,
        boolean carry,
        long seed) {

    public WorksheetSpec {
        if (grade < 1 || grade > 6) {
            throw new IllegalArgumentException("초등 1~6학년만 지원합니다: " + grade);
        }
        if (count < 1 || count > 50) {
            throw new IllegalArgumentException("한 번에 1~50문항까지 만들 수 있습니다: " + count);
        }
        categories = List.copyOf(categories);
    }

    public String code() {
        return ReadableCode.encode(seed);
    }
}
