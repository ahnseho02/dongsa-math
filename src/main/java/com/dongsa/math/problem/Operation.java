package com.dongsa.math.problem;

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

    public int apply(java.util.List<Integer> operands) {
        int result = operands.get(0);
        for (int i = 1; i < operands.size(); i++) {
            int n = operands.get(i);
            result = switch (this) {
                case ADD -> result + n;
                case SUBTRACT -> result - n;
                case MULTIPLY -> result * n;
                case DIVIDE -> result / n;
            };
        }
        return result;
    }
}
