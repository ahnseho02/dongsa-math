package com.dongsa.math.problem;

/**
 * 숫자를 어떻게 뽑을지. 문장 템플릿은 DB 에 있지만 숫자를 만드는 방법은 코드에 있다.
 * 조건(받아올림 여부, 답이 음수가 아닐 것, 나누어떨어질 것)을 지켜야 하기 때문이다.
 */
public enum NumberPattern {

    /** 두 수 덧셈. 받아올림 유무를 맞춘다. */
    ADD_PAIR,
    /** 비교형 덧셈. 두 번째 수(차이)는 작게 뽑는다. "145권보다 38권 더 많이" */
    ADD_SMALL_SECOND,
    /** 두 수 뺄셈. 앞의 수가 항상 크고, 받아내림 유무를 맞춘다. */
    SUB_PAIR,
    /** 세 수 덧셈. */
    ADD_TRIPLE,
    /** 곱셈. 묶음 수 × 한 묶음의 개수. */
    MUL_PAIR,
    /** 나눗셈. 항상 나누어떨어지게 만든다. */
    DIV_PAIR,

    // ── 초4~6: 소수 ──

    /** 소수 덧셈. 초4 는 소수 첫째 자리, 초5 부터 둘째 자리까지. */
    DECIMAL_ADD_PAIR,
    /** 소수 뺄셈. 앞의 수가 항상 크다. */
    DECIMAL_SUB_PAIR,
    /** (소수) × (자연수). 초5. */
    DECIMAL_MUL_WHOLE,
    /** (소수) ÷ (자연수). 초6. 몫이 딱 떨어지게 만든다. */
    DECIMAL_DIV_WHOLE,
    /** 소수 세 개 더하기. */
    DECIMAL_ADD_TRIPLE,

    // ── 초4~6: 분수 ──

    /** 분모가 같은 분수의 덧셈. 초4. 합이 1 을 넘지 않게 해서 답이 진분수로 나온다. */
    FRACTION_ADD_SAME,
    /** 분모가 같은 분수의 뺄셈. 초4. */
    FRACTION_SUB_SAME,
    /** 분모가 다른 분수의 덧셈. 초5 — 통분이 필요하다. */
    FRACTION_ADD_DIFF,
    /** 분모가 다른 분수의 뺄셈. 초5. */
    FRACTION_SUB_DIFF,
    /** 분수끼리 곱하기. 초5. */
    FRACTION_MUL,
    /** 분수끼리 나누기. 초6. */
    FRACTION_DIV,

    // ── 초6: 비율 ──

    /** 전체의 몇 퍼센트. 식은 (전체) × (소수로 고친 비율) 이 된다. */
    PERCENT_OF
}
