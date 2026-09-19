package com.dongsa.math.game;

import com.dongsa.math.problem.Operation;

/** 게임 문제 하나. 문장 없이 숫자와 기호만 있다 — 계산 속도를 보는 것이 목적이다. */
public record GameProblem(int no, int left, int right, Operation operation, int answer) {

    public String expression() {
        return left + " " + operation.sign() + " " + right;
    }
}
