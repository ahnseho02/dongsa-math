package com.dongsa.math.problem;

/**
 * 한 문제를 푸는 네 단계.
 *
 * 답만 물으면 "계산은 되는데 문장을 못 읽는 아이"와 "문장은 읽는데 계산을 틀리는 아이"가
 * 똑같은 점수로 나온다. 단계를 나눠 물어야 둘이 갈린다. 이 서비스의 핵심이다.
 */
public enum SolveStep {

    /** 문장에서 연산을 알려 주는 단서 단어 찾기 */
    CUE("단서", 1),
    /** 그 단서가 어떤 연산인지 고르기 */
    OPERATION("연산", 2),
    /** 숫자를 식으로 옮기기 */
    EXPRESSION("식", 3),
    /** 계산해서 답 내기 */
    ANSWER("답", 4);

    private final String label;
    private final int order;

    SolveStep(String label, int order) {
        this.label = label;
        this.order = order;
    }

    public String label() { return label; }

    public int order() { return order; }

    public boolean comesAfter(SolveStep other) {
        return this.order > other.order;
    }

    public static final int COUNT = values().length;
}
