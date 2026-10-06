package com.dongsa.math.problem;

import java.util.List;

public enum Operation {

    ADD("+"), SUBTRACT("−"), MULTIPLY("×"), DIVIDE("÷");

    private final String sign;

    Operation(String sign) {
        this.sign = sign;
    }

    public String sign() {
        return sign;
    }

    /** 뺄셈과 나눗셈은 순서가 바뀌면 답이 달라진다. 덧셈·곱셈은 순서를 따지지 않는다. */
    public boolean orderMatters() {
        return this == SUBTRACT || this == DIVIDE;
    }

    public Num apply(List<Num> operands) {
        Num result = operands.get(0);
        for (int i = 1; i < operands.size(); i++) {
            Num n = operands.get(i);
            result = switch (this) {
                case ADD -> result.plus(n);
                case SUBTRACT -> result.minus(n);
                case MULTIPLY -> result.times(n);
                case DIVIDE -> result.dividedBy(n);
            };
        }
        return result;
    }
}
